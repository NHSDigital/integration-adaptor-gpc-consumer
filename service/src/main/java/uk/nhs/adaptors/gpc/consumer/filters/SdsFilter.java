package uk.nhs.adaptors.gpc.consumer.filters;

import static uk.nhs.adaptors.gpc.consumer.utils.HeaderConstants.SSP_TRACE_ID;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.function.TriFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import uk.nhs.adaptors.gpc.consumer.sds.SdsClient;
import uk.nhs.adaptors.gpc.consumer.sds.exception.SdsException;
import uk.nhs.adaptors.gpc.consumer.utils.LoggingUtil;
import uk.nhs.adaptors.gpc.consumer.utils.QueryParamsEncoder;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SdsFilter implements GlobalFilter, Ordered {
    public static final int SDS_FILTER_ORDER = RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1;

    @Value("${gpc-consumer.sds.enableSDS}")
    private String enableSds;

    private static final String INTERACTION_ID_PREFIX = "urn:nhs:names:services:gpconnect:";
    private static final String STRUCTURED_ID = INTERACTION_ID_PREFIX + "fhir:operation:gpc.getstructuredrecord-1";
    private static final String PATIENT_SEARCH_ID = INTERACTION_ID_PREFIX + "documents:fhir:rest:search:patient-1";
    private static final String DOCUMENT_SEARCH_ID = INTERACTION_ID_PREFIX + "documents:fhir:rest:search:documentreference-1";
    private static final String BINARY_READ_ID = INTERACTION_ID_PREFIX + "documents:fhir:rest:read:binary-1";
    private static final String SSP_INTERACTION_ID = "Ssp-InteractionID";
    private static final String DOCUMENT_REFERENCE_SUFFIX = "/DocumentReference";
    private static final int SDS_URI_OFFSET = 8;

    private final SdsClient sdsClient;

    private Map<String, TriFunction<String, String, ServerWebExchange, Mono<SdsClient.SdsResponseData>>> sdsRequestFunctions;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest serverHttpRequest = exchange.getRequest();

        return Mono.just(enableSds)
            .map(Boolean::valueOf)
            .map(isSdsEnabled -> {
                if (isSdsEnabled) {
                    LoggingUtil.info(LOGGER, exchange, "SDS is enabled. Using SDS API for service discovery");
                    var id = extractInteractionId(serverHttpRequest.getHeaders());
                    return proceedSdsLookup(exchange, id.get());
                } else {
                    return SdsClient.SdsResponseData.builder().build();
                }
            }).doOnNext(v -> {
                if (serverHttpRequest.getPath().value().endsWith(DOCUMENT_REFERENCE_SUFFIX)) {
                    QueryParamsEncoder.encodeQueryParams(exchange);
                }
            }).then(chain.filter(exchange));
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
            BINARY_READ_ID, sdsClient::callForRetrieveDocumentRecord);
    }

    private Mono<SdsClient.SdsResponseData> proceedSdsLookup(ServerWebExchange exchange, String integrationId) {
        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        String organisation = extractOrganisation(serverHttpRequest.getPath());
        var sspTraceId = extractSspTraceId(exchange.getRequest().getHeaders());
        return performRequestAccordingToInteractionId(integrationId, organisation, sspTraceId, exchange)
            .switchIfEmpty(Mono.error(new SdsException(
                    String.format("No endpoint found in SDS for GP Connect endpoint InteractionId=%s OdsCode=%s",
                        integrationId,
                        organisation)))
            ).doOnNext(response -> {
                LoggingUtil.info(LOGGER, exchange, "Found GP connect provider endpoint in sds: {}", response.getAddress());
                prepareLookupUri(response.getAddress(), serverHttpRequest)
                    .ifPresent(uri -> exchange.getAttributes()
                        .put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, uri));
            });

    }

    private Mono<SdsClient.SdsResponseData> performRequestAccordingToInteractionId(String interactionId,
            String organisation, String sspTraceId, ServerWebExchange exchange) {
        if (sdsRequestFunctions.containsKey(interactionId)) {
            LoggingUtil.info(LOGGER, exchange, "Performing request with organisation \"{}\" and NHS service endpoint id \"{}\"",
                organisation, interactionId);
            return sdsRequestFunctions.get(interactionId)
                .apply(organisation, sspTraceId, exchange);
        }
        throw new IllegalArgumentException(String.format("Not recognised InteractionId %s", interactionId));
    }

    private String extractOrganisation(RequestPath requestPath) {
        return requestPath.elements()
            .stream()
            .skip(1)
            .findFirst()
            .map(PathContainer.Element::value)
            .orElseThrow(() -> new IllegalArgumentException("URL does not contain ODS code in its second element"));
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

    private String extractSspTraceId(HttpHeaders httpHeaders) {
        if (httpHeaders.containsKey(SSP_TRACE_ID)) {
            List<String> sspTraceIds = httpHeaders.get(SSP_TRACE_ID);

            if (!CollectionUtils.isEmpty(sspTraceIds)) {
                return sspTraceIds.get(0);
            }
        }
        throw new SdsException("Missing Ssp-TraceID Header for X-Correlation-Id for SDS Request");
    }

    private Optional<URI> prepareLookupUri(String address, ServerHttpRequest serverHttpRequest) {
        String uri = address + serverHttpRequest.getPath().subPath(SDS_URI_OFFSET)
            .toString()
            .substring(1);
        URI constructedUri = UriComponentsBuilder.fromUriString(uri)
            .queryParams(serverHttpRequest.getQueryParams())
            .build()
            .toUri();
        return Optional.of(constructedUri);
    }
}
