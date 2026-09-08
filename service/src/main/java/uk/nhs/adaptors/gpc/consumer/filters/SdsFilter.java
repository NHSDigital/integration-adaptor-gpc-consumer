package uk.nhs.adaptors.gpc.consumer.filters;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import uk.nhs.adaptors.gpc.consumer.filters.exception.SdsFilterException;
import uk.nhs.adaptors.gpc.consumer.sds.SdsClient;
import uk.nhs.adaptors.gpc.consumer.sds.exception.SdsException;
import uk.nhs.adaptors.gpc.consumer.utils.LoggingUtil;
import uk.nhs.adaptors.gpc.consumer.utils.OperationOutcomes;
import uk.nhs.adaptors.gpc.consumer.utils.QueryParamsEncoder;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

import static uk.nhs.adaptors.gpc.consumer.gpc.InteractionIds.DOCUMENT_MIGRATE_ID;
import static uk.nhs.adaptors.gpc.consumer.gpc.InteractionIds.DOCUMENT_READ_ID;
import static uk.nhs.adaptors.gpc.consumer.gpc.InteractionIds.DOCUMENT_SEARCH_ID;
import static uk.nhs.adaptors.gpc.consumer.gpc.InteractionIds.MIGRATE_STRUCTURED_ID;
import static uk.nhs.adaptors.gpc.consumer.gpc.InteractionIds.PATIENT_SEARCH_ID;
import static uk.nhs.adaptors.gpc.consumer.gpc.InteractionIds.STRUCTURED_ID;
import static uk.nhs.adaptors.gpc.consumer.utils.HeaderConstants.SSP_FROM;
import static uk.nhs.adaptors.gpc.consumer.utils.HeaderConstants.SSP_INTERACTION_ID;
import static uk.nhs.adaptors.gpc.consumer.utils.HeaderConstants.SSP_TO;
import static uk.nhs.adaptors.gpc.consumer.utils.HeaderConstants.SSP_TRACE_ID;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SdsFilter implements GlobalFilter, Ordered {

    public static final int SDS_FILTER_ORDER = RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1;
    private static final String DOCUMENT_REFERENCE_SUFFIX = "/DocumentReference";
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    public static final String EXCEPTION = "exception";
    public static final String STRUCTURE = "structure";
    public static final String NOT_FOUND = "not-found";
    public static final String BAD_GATEWAY = "BAD_GATEWAY";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String PATIENT_NOT_FOUND = "PATIENT_NOT_FOUND";
    public static final String MISSING_HEADER_EXCEPTION_MESSAGE = "Missing or empty %s Header Value for SDS Request";

    private final SdsClient sdsClient;
    private Map<String, BiFunction<String, String, Mono<SdsClient.SdsResponseData>>> sdsRequestFunctions;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return getGpcProviderEndpointDetails(exchange)
                .flatMap(gpcProviderEndpointDetails -> appendSspHeadersToExchangeIfRequired(
                        exchange,
                        chain,
                        gpcProviderEndpointDetails)
                )
                .onErrorResume(Exception.class, e -> buildErrorResponse(exchange, e));
    }

    private Mono<SdsClient.SdsResponseData> getGpcProviderEndpointDetails(ServerWebExchange exchange) {
        return performGpcProviderSdsLookup(exchange)
                .doOnNext(v -> encodeQueryParamsToUrlWhenDocumentRequest(exchange));
    }

    private static void encodeQueryParamsToUrlWhenDocumentRequest(ServerWebExchange exchange) {
        if (exchange.getRequest().getPath().value().endsWith(DOCUMENT_REFERENCE_SUFFIX)) {
            QueryParamsEncoder.encodeQueryParams(exchange);
        }
    }

    @NotNull
    private Mono<SdsClient.SdsResponseData> performGpcProviderSdsLookup(ServerWebExchange exchange) {
        LoggingUtil.info(LOGGER, exchange, "Using SDS API for GP connect provider service lookup");

        var id = extractHeaderValueOrThrowSdsException(exchange.getRequest().getHeaders(), SSP_INTERACTION_ID);
        return performGpcProviderSdsLookup(exchange, id);
    }

    private String extractHeaderValueOrThrowSdsException(HttpHeaders httpHeaders, String headerName) {
        return Optional.ofNullable(httpHeaders.getFirst(headerName))
                .filter(StringUtils::hasText)
                .orElseThrow(() -> new SdsException(MISSING_HEADER_EXCEPTION_MESSAGE.formatted(headerName)));
    }

    private Mono<SdsClient.SdsResponseData> performGpcProviderSdsLookup(ServerWebExchange exchange, String interactionId) {
        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        String organisation = extractOdsCode(serverHttpRequest.getPath());
        var sspTraceId = extractHeaderValueOrThrowSdsException(exchange.getRequest().getHeaders(), SSP_TRACE_ID);

        return performRequestAccordingToInteractionId(interactionId, organisation, sspTraceId, exchange)
                .switchIfEmpty(buildNoEndpointFoundError(interactionId, organisation)
                ).doOnNext(response -> updateGatewayRequestUrlIfEndpointFound(
                        exchange,
                        response,
                        serverHttpRequest
                ));
    }

    private void updateGatewayRequestUrlIfEndpointFound(
            ServerWebExchange exchange,
            SdsClient.SdsResponseData response,
            ServerHttpRequest serverHttpRequest
    ) {
        LoggingUtil.info(LOGGER, exchange, "Found GP connect provider endpoint in sds: {}", response.getAddress());
        prepareLookupUri(response.getAddress(), serverHttpRequest)
                .ifPresent(uri -> exchange.getAttributes()
                        .put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, uri));
    }

    private static @NotNull Mono<SdsClient.SdsResponseData> buildNoEndpointFoundError(String interactionId, String organisation) {
        return Mono.error(new SdsException(
                "No endpoint found in SDS for GP Connect endpoint InteractionId=%s OdsCode=%s".formatted(
                        interactionId,
                        organisation)));
    }

    private Mono<Void> appendSspHeadersToExchangeIfRequired(
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            SdsClient.SdsResponseData gpcProviderEndpointDetails
    ) {
        return gpcProviderEndpointDetails == null
                ? chain.filter(exchange)
                : getGpcConsumerAsid(exchange)
                    .flatMap(gpcConsumerAsid -> addMissingSspHeaders(
                        gpcConsumerAsid,
                        exchange,
                        chain,
                        gpcProviderEndpointDetails
                    ));
    }

    private Mono<? extends Void> addMissingSspHeaders(
            String gpcConsumerAsid,
            ServerWebExchange exchange,
            GatewayFilterChain chain,
            SdsClient.SdsResponseData gpcProviderEndpointDetails
    ) {
        var mutatedExchange = appendSspHeaderWhenAbsent(exchange, gpcProviderEndpointDetails.getNhsSpineAsid(), SSP_TO);
        mutatedExchange = appendSspHeaderWhenAbsent(mutatedExchange, gpcConsumerAsid, SSP_FROM);
        return chain.filter(mutatedExchange);
    }

    private static @NotNull Mono<Void> buildErrorResponse(ServerWebExchange exchange, Exception e) {
        HttpStatus status = mapExceptionToHttpStatus(e);
        String spineCode = mapWebClientExceptionToSpineCode(e);
        String fhirCode = mapSpineCodeToFhirCode(spineCode);

        ResponseEntity<String> errorResponse = OperationOutcomes.buildErrorResponse(status, spineCode, fhirCode, e.getMessage());
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(errorResponse.getStatusCode());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = Objects.requireNonNullElse(errorResponse.getBody(), "");
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private static HttpStatus mapExceptionToHttpStatus(Exception e) {
        return switch (e) {
            case WebClientRequestException ignored -> HttpStatus.BAD_GATEWAY;
            case WebClientResponseException webClientResponseEx -> HttpStatus.resolve(webClientResponseEx.getStatusCode().value());
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private static String mapWebClientExceptionToSpineCode(Exception e) {
        if (e instanceof WebClientResponseException ex) {
            return switch (ex.getStatusCode()) {
                case HttpStatus.NOT_FOUND -> PATIENT_NOT_FOUND;
                case HttpStatus.BAD_REQUEST -> BAD_REQUEST;
                case HttpStatus.BAD_GATEWAY -> BAD_GATEWAY;
                default -> INTERNAL_SERVER_ERROR;
            };
        }
        return INTERNAL_SERVER_ERROR;
    }

    private static String mapSpineCodeToFhirCode(String spineCode) {
        return switch (spineCode) {
            case BAD_REQUEST -> STRUCTURE;
            case PATIENT_NOT_FOUND -> NOT_FOUND;
            default -> EXCEPTION;
        };
    }

    private Mono<String> getGpcConsumerAsid(ServerWebExchange exchange) {
        LoggingUtil.info(LOGGER, exchange, "Using SDS API to fetch GPC consumer ASID value");

        var odsCode = extractOdsCode(exchange.getRequest().getPath());
        var correlationId = extractHeaderValueOrThrowSdsException(exchange.getRequest().getHeaders(), SSP_TRACE_ID);
        var interactionId = extractHeaderValueOrThrowSdsException(exchange.getRequest().getHeaders(), SSP_INTERACTION_ID);

        return sdsClient.callForGetAsid(interactionId, odsCode, correlationId);
    }

    private String extractOdsCode(RequestPath requestPath) {
        return requestPath.elements().stream()
                .skip(1)
                .findFirst()
                .map(PathContainer.Element::value)
                .orElseThrow(() -> new IllegalArgumentException("URL does not contain ODS code in its second element"));
    }

    @NotNull
    private ServerWebExchange appendSspHeaderWhenAbsent(ServerWebExchange exchange, String asid, String sspHeader) {
        String ssp = Optional.ofNullable(exchange.getRequest().getHeaders().getFirst(sspHeader))
                .filter(StringUtils::hasText)
                .orElse(asid);

        ServerHttpRequest mutateRequest = exchange.getRequest().mutate().header(sspHeader, ssp).build();

        return exchange.mutate().request(mutateRequest).build();
    }


    @Override
    public int getOrder() {
        return RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1;
    }

    @PostConstruct
    @SuppressWarnings("unused")
    public void initializeSdsRequestFunctions() {
        sdsRequestFunctions = Map.of(
                STRUCTURED_ID, sdsClient::callForGetStructuredRecord,
                PATIENT_SEARCH_ID, sdsClient::callForPatientSearchAccessDocument,
                DOCUMENT_SEARCH_ID, sdsClient::callForSearchForDocumentRecord,
                DOCUMENT_READ_ID, sdsClient::callForRetrieveDocumentRecord,
                DOCUMENT_MIGRATE_ID, sdsClient::callForMigrateDocumentRecord,
                MIGRATE_STRUCTURED_ID, sdsClient::callForMigrateStructuredRecord
        );
    }

    private Mono<SdsClient.SdsResponseData> performRequestAccordingToInteractionId(
            String interactionId,
            String organisation,
            String sspTraceId,
            ServerWebExchange exchange
    ) {
        if (sdsRequestFunctions.containsKey(interactionId)) {
            LoggingUtil.info(LOGGER, exchange, "Performing request with organisation \"{}\" and NHS service endpoint id \"{}\"",
                    organisation, interactionId);
            return sdsRequestFunctions.get(interactionId)
                    .apply(organisation, sspTraceId);
        }
        throw new IllegalArgumentException(String.format("Not recognised InteractionId %s", interactionId));
    }

    private Optional<URI> prepareLookupUri(String serviceRootUrl, ServerHttpRequest originalRequest) {
        var originalRequestPath = originalRequest.getPath();
        var originalRequestPathValues = originalRequestPath.elements().stream()
                .map(PathContainer.Element::value)
                .toList();
        int indexOfPatientInFhirPath = originalRequestPathValues.lastIndexOf("Patient");
        int indexOfBinaryInFhirPath = originalRequestPathValues.lastIndexOf("Binary");
        int indexOfStartOfFhirPath = Math.max(indexOfPatientInFhirPath, indexOfBinaryInFhirPath);
        if (indexOfStartOfFhirPath < 0) {
            throw new SdsFilterException("Unable to detect a supported FHIR path in the original request");
        }
        String fhirRequestPathPart = originalRequest.getPath().subPath(indexOfStartOfFhirPath - 1)
                .toString();
        String uriWithoutQueryParameters = serviceRootUrl + fhirRequestPathPart;
        URI constructedUri = UriComponentsBuilder.fromUriString(uriWithoutQueryParameters)
                .queryParams(originalRequest.getQueryParams())
                .build()
                .toUri();
        return Optional.of(constructedUri);
    }
}