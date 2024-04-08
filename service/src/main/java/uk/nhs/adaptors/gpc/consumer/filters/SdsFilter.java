package uk.nhs.adaptors.gpc.consumer.filters;

import static uk.nhs.adaptors.gpc.consumer.gpc.InteractionIds.DOCUMENT_MIGRATE_ID;
import static uk.nhs.adaptors.gpc.consumer.gpc.InteractionIds.DOCUMENT_READ_ID;
import static uk.nhs.adaptors.gpc.consumer.gpc.InteractionIds.DOCUMENT_SEARCH_ID;
import static uk.nhs.adaptors.gpc.consumer.gpc.InteractionIds.MIGRATE_STRUCTURED_ID;
import static uk.nhs.adaptors.gpc.consumer.gpc.InteractionIds.PATIENT_SEARCH_ID;
import static uk.nhs.adaptors.gpc.consumer.gpc.InteractionIds.STRUCTURED_ID;
import static uk.nhs.adaptors.gpc.consumer.utils.HeaderConstants.SSP_TRACE_ID;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import uk.nhs.adaptors.gpc.consumer.filters.exception.SdsFilterException;
import uk.nhs.adaptors.gpc.consumer.sds.SdsClient;
import uk.nhs.adaptors.gpc.consumer.sds.exception.SdsException;
import uk.nhs.adaptors.gpc.consumer.utils.LoggingUtil;
import uk.nhs.adaptors.gpc.consumer.utils.QueryParamsEncoder;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SdsFilter implements GlobalFilter, Ordered {

    public static final int SDS_FILTER_ORDER = RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1;
    public static final String SSP_INTERACTION_ID = "Ssp-InteractionID";
    private static final String DOCUMENT_REFERENCE_SUFFIX = "/DocumentReference";
    private final SdsClient sdsClient;
    private Map<String, BiFunction<String, String, Mono<SdsClient.SdsResponseData>>> sdsRequestFunctions;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return getGpcProviderEndpointDetails(exchange).flatMap(gpcProviderEndpointDetails -> {
            if (gpcProviderEndpointDetails != null) {
                return getGpcConsumerAsid(exchange).flatMap(gpcConsumerAsid -> {
                    var mutatedExchange = appendSspHeaderWhenAbsent(exchange, gpcProviderEndpointDetails.getNhsSpineAsid(), "Ssp-To");
                    mutatedExchange = appendSspHeaderWhenAbsent(mutatedExchange, gpcConsumerAsid, "Ssp-From");

                    return chain.filter(mutatedExchange);
                });
            }

            return chain.filter(exchange);
        });
    }

    private Mono<String> getGpcConsumerAsid(ServerWebExchange exchange) {
        LoggingUtil.info(LOGGER, exchange, "Using SDS API to fetch GPC consumer ASID value");

        var odsCode = extractOrganisation(exchange.getRequest().getPath());
        var correlationId = extractSspTraceId(exchange.getRequest().getHeaders());
        var interactionId = extractInteractionId(exchange.getRequest().getHeaders());

        return sdsClient.callForGetAsid(interactionId.get(), odsCode, correlationId);
    }

    @NotNull
    private ServerWebExchange appendSspHeaderWhenAbsent(ServerWebExchange exchange, String asid, String sspHeader) {

        List<String> incomingSspHeaderValue = exchange.getRequest().getHeaders().get(sspHeader);
        String ssp = asid;

        if (incomingSspHeaderValue != null) {
            ssp = incomingSspHeaderValue.stream().findFirst().orElse(asid);
        }

        ServerHttpRequest mutateRequest = exchange.getRequest()
            .mutate()
            .header(sspHeader, ssp)
            .build();

        return exchange.mutate().request(mutateRequest).build();
    }

    private Mono<SdsClient.SdsResponseData> getGpcProviderEndpointDetails(ServerWebExchange exchange) {

        return performGpcProviderSdsLookup(exchange)
            .doOnNext(v -> {
                if (exchange.getRequest().getPath().value().endsWith(DOCUMENT_REFERENCE_SUFFIX)) {
                    QueryParamsEncoder.encodeQueryParams(exchange);
                }
            });
    }

    @NotNull
    private Mono<SdsClient.SdsResponseData> performGpcProviderSdsLookup(ServerWebExchange exchange) {

        LoggingUtil.info(LOGGER, exchange, "Using SDS API for GP connect provider service lookup");

        var id = extractInteractionId(exchange.getRequest().getHeaders());
        return performGpcProviderSdsLookup(exchange, id.get());
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

    private Optional<String> extractInteractionId(HttpHeaders httpHeaders) {
        if (httpHeaders.containsKey(SSP_INTERACTION_ID)) {
            List<String> interactionIds = httpHeaders.get(SSP_INTERACTION_ID);

            if (!CollectionUtils.isEmpty(interactionIds)) {
                return Optional.of(interactionIds.get(0));
            }
        }
        return Optional.empty();
    }

    private Mono<SdsClient.SdsResponseData> performGpcProviderSdsLookup(ServerWebExchange exchange, String interactionId) {

        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        String organisation = extractOrganisation(serverHttpRequest.getPath());
        var sspTraceId = extractSspTraceId(exchange.getRequest().getHeaders());

        return performRequestAccordingToInteractionId(interactionId, organisation, sspTraceId, exchange)
            .switchIfEmpty(Mono.error(new SdsException(
                String.format("No endpoint found in SDS for GP Connect endpoint InteractionId=%s OdsCode=%s",
                    interactionId,
                    organisation)))
            ).doOnNext(response -> {
                LoggingUtil.info(LOGGER, exchange, "Found GP connect provider endpoint in sds: {}", response.getAddress());
                prepareLookupUri(response.getAddress(), serverHttpRequest)
                    .ifPresent(uri -> exchange.getAttributes()
                        .put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, uri));
            });
    }

    private String extractOrganisation(RequestPath requestPath) {
        return requestPath.elements()
            .stream()
            .skip(1)
            .findFirst()
            .map(PathContainer.Element::value)
            .orElseThrow(() -> new IllegalArgumentException("URL does not contain ODS code in its second element"));
    }

    private String extractSspTraceId(HttpHeaders httpHeaders) {
        if (httpHeaders.containsKey(SSP_TRACE_ID)) {
            List<String> sspTraceIds = httpHeaders.get(SSP_TRACE_ID);

            if (!CollectionUtils.isEmpty(sspTraceIds)) {
                return sspTraceIds.get(0);
            }
        }
        throw new SdsException("Missing Ssp-TraceID Header for X-Correlation-Id for SDS Request");
    }

    private Mono<SdsClient.SdsResponseData> performRequestAccordingToInteractionId(String interactionId,
        String organisation, String sspTraceId, ServerWebExchange exchange) {
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
            .collect(Collectors.toList());
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
