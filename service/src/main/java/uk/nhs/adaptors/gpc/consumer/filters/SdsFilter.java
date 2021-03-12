package uk.nhs.adaptors.gpc.consumer.filters;

import java.net.URI;
import java.net.URISyntaxException;
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
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ServerWebExchange;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import uk.nhs.adaptors.gpc.consumer.sds.SdsClient;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SdsFilter implements GlobalFilter, Ordered {
    private static final String GPC_URL_ENVIRONMENT_VARIABLE = "GP2GP_GPC_GET_URL";
    private static final String INTERACTION_ID_PREFIX = "urn:nhs:names:services:gpconnect:";
    private static final String STRUCTURED_ID = INTERACTION_ID_PREFIX + "fhir:operation:gpc.getstructuredrecord-1";
    private static final String PATIENT_SEARCH_ID = INTERACTION_ID_PREFIX + "documents:fhir:rest:search:patient-1";
    private static final String DOCUMENT_SEARCH_ID = INTERACTION_ID_PREFIX + "documents:fhir:rest:search:documentreference-1";
    private static final String BINARY_READ_ID = INTERACTION_ID_PREFIX + "documents:fhir:rest:read:binary-1";
    private static final String SSP_INTERACTION_ID = "Ssp-InteractionID";
    private static final String SLASH = "/";
    private static final String FHIR_SEPARATOR = "fhir/";
    private static final String DOCUMENTS_SEPARATOR = "documents/";
    private static final String DOUBLE_SEPARATOR = DOCUMENTS_SEPARATOR + FHIR_SEPARATOR;

    private final SdsClient sdsClient;

    private Map<String, Function<String, Optional<SdsClient.SdsResponseData>>> sdsRequestFunctions;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (StringUtils.isBlank(System.getProperty(GPC_URL_ENVIRONMENT_VARIABLE))) {
            ServerHttpRequest serverHttpRequest = exchange.getRequest();
            extractInteractionId(serverHttpRequest.getHeaders())
                .ifPresent(id -> proceedSdsLookup(serverHttpRequest, exchange, id));
        }

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

    private void proceedSdsLookup(ServerHttpRequest serverHttpRequest,
            ServerWebExchange exchange,
            String integrationId) {
        Optional<SdsClient.SdsResponseData> response
            = performRequestAccordingToInteractionId(integrationId, serverHttpRequest);

        if (response.isPresent()) {
            String address = response.get()
                .getAddress();
            prepareLookupUri(address, serverHttpRequest.getPath()).ifPresent(uri -> exchange.getAttributes()
                .put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, uri));
        } else {
            LOGGER.error("SDS filter request was unsuccessful.");
        }
    }

    private Optional<SdsClient.SdsResponseData> performRequestAccordingToInteractionId(String interactionId,
            ServerHttpRequest serverHttpRequest) {
        if (isRequestFunctionAvailable(interactionId)) {
            Optional<String> organisation = extractOrganisation(serverHttpRequest
                .getPath());

            if (organisation.isPresent()) {
                LOGGER.info("Performing request with organisation \"{}\" and NHS service endpoint id \"{}\"",
                    organisation.get(), interactionId);
                return sdsRequestFunctions.get(interactionId)
                    .apply(organisation.get());
            }
        }
        return Optional.empty();
    }

    private boolean isRequestFunctionAvailable(String interactionId) {
        return sdsRequestFunctions.keySet()
            .stream()
            .anyMatch(key -> key.equals(interactionId));
    }

    private Optional<String> extractOrganisation(RequestPath requestPath) {
        String path = requestPath.toString();
        if (path.contains(SLASH)) {
            String[] pathElements = path.split(SLASH);

            if (pathElements.length > 1) {
                return Optional.of(pathElements[1]);
            }
        }
        return Optional.empty();
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

    private Optional<URI> prepareLookupUri(String address, RequestPath requestPath) {
        String path = requestPath.value();
        if (StringUtils.isNotBlank(address)) {
            try {
                String uri = address + extractUriSuffix(path);
                return Optional.of(new URI(uri));
            } catch (URISyntaxException e) {
                LOGGER.error("Invalid address in SDS: " + address);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private String extractUriSuffix(String path) {
        if (path.contains(DOUBLE_SEPARATOR)) {
            return path.split(DOUBLE_SEPARATOR)[1];
        } else if (path.contains(FHIR_SEPARATOR)) {
            return path.split(FHIR_SEPARATOR)[1];
        } else if (path.contains(DOCUMENTS_SEPARATOR)) {
            return path.split(DOCUMENTS_SEPARATOR)[1];
        }
        return StringUtils.EMPTY;
    }
}
