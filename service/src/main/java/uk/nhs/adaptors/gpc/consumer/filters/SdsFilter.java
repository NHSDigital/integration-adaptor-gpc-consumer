package uk.nhs.adaptors.gpc.consumer.filters;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
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
import uk.nhs.adaptors.gpc.consumer.sds.SdsClient;
import uk.nhs.adaptors.gpc.consumer.sds.exception.SdsException;
import uk.nhs.adaptors.gpc.consumer.utils.QueryParamsEncoder;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SdsFilter implements GlobalFilter, Ordered {
    public static final int SDS_FILTER_ORDER = RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1;

    private static final String GPC_URL_ENVIRONMENT_VARIABLE = "GPC_CONSUMER_GPC_GET_URL";
    private static final String INTERACTION_ID_PREFIX = "urn:nhs:names:services:gpconnect:";
    private static final String STRUCTURED_ID = INTERACTION_ID_PREFIX + "fhir:operation:gpc.getstructuredrecord-1";
    private static final String PATIENT_SEARCH_ID = INTERACTION_ID_PREFIX + "documents:fhir:rest:search:patient-1";
    private static final String DOCUMENT_SEARCH_ID = INTERACTION_ID_PREFIX + "documents:fhir:rest:search:documentreference-1";
    private static final String BINARY_READ_ID = INTERACTION_ID_PREFIX + "documents:fhir:rest:read:binary-1";
    private static final String SSP_INTERACTION_ID = "Ssp-InteractionID";
    private static final int SDS_URI_OFFSET = 8;

    private final SdsClient sdsClient;

    private Map<String, Function<String, Optional<SdsClient.SdsResponseData>>> sdsRequestFunctions;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (StringUtils.isBlank(System.getenv(GPC_URL_ENVIRONMENT_VARIABLE))) {
            ServerHttpRequest serverHttpRequest = exchange.getRequest();
            extractInteractionId(serverHttpRequest.getHeaders())
                .ifPresent(id -> proceedSdsLookup(exchange, id));
        }
        QueryParamsEncoder.encodeQueryParams(exchange);

        return chain.filter(exchange);
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

    private void proceedSdsLookup(ServerWebExchange exchange, String integrationId) {
        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        String organisation = extractOrganisation(serverHttpRequest.getPath());
        SdsClient.SdsResponseData response = performRequestAccordingToInteractionId(integrationId, organisation)
            .orElseThrow(() -> new SdsException(
                String.format("No endpoint found in SDS for GP Connect endpoint InteractionId=%s OdsCode=%s",
                    integrationId,
                    organisation))
            );
        prepareLookupUri(response.getAddress(), serverHttpRequest)
            .ifPresent(uri -> exchange.getAttributes()
                .put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, uri));
    }

    private Optional<SdsClient.SdsResponseData> performRequestAccordingToInteractionId(String interactionId,
            String organisation) {
        if (sdsRequestFunctions.containsKey(interactionId)) {
            LOGGER.info("Performing request with organisation \"{}\" and NHS service endpoint id \"{}\"",
                organisation, interactionId);
            return sdsRequestFunctions.get(interactionId)
                .apply(organisation);
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
