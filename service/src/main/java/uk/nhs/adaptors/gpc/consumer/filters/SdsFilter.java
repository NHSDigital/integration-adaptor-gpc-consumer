package uk.nhs.adaptors.gpc.consumer.filters;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.PostConstruct;

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

import io.micrometer.core.instrument.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import uk.nhs.adaptors.gpc.consumer.sds.SdsClient;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SdsFilter implements GlobalFilter, Ordered {
    private static final String INTERACTION_ID_PREFIX = "urn:nhs:names:services:gpconnect:";
    private static final String STRUCTURED_ID = INTERACTION_ID_PREFIX + "fhir:operation:gpc.getstructuredrecord-1";
    private static final String PATIENT_SEARCH_ID = INTERACTION_ID_PREFIX + "documents:fhir:rest:search:patient-1";
    private static final String DOCUMENT_SEARCH_ID = INTERACTION_ID_PREFIX + "documents:fhir:rest:search:documentreference-1";
    private static final String BINARY_READ_ID = INTERACTION_ID_PREFIX + "documents:fhir:rest:read:binary-1";
    private static final String SSP_INTERACTION_ID = "Ssp-InteractionID";
    private static final String SLASH = "/";
    private static final String SCHEMA_SEPARATOR = "://";
    private static final String COLON = ":";

    private final SdsClient sdsClient;

    private Map<String, Function<String, Optional<SdsClient.SdsResponseData>>> sdsRequestFunctions;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        Optional<SdsClient.SdsResponseData> response = handleRequest(serverHttpRequest);

        if (response.isPresent()) {
            String address = response.get()
                .getAddress();
            prepareLookupUri(address, serverHttpRequest.getPath()).ifPresent(uri -> exchange.getAttributes()
                .put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, uri));
        } else {
            LOGGER.error("SDS filter request was unsuccessful.");
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1;
    }

    @PostConstruct
    public void initializeFunctions() {
        sdsRequestFunctions = Map.of(
            STRUCTURED_ID, sdsClient::callForGetStructuredRecord,
            PATIENT_SEARCH_ID, sdsClient::callForPatientSearchAccessDocument,
            DOCUMENT_SEARCH_ID, sdsClient::callForSearchForDocumentRecord,
            BINARY_READ_ID, sdsClient::callForRetrieveDocumentRecord);
    }

    private Optional<SdsClient.SdsResponseData> handleRequest(ServerHttpRequest serverHttpRequest) {
        HttpHeaders httpHeaders = serverHttpRequest.getHeaders();

        if (httpHeaders.containsKey(SSP_INTERACTION_ID)) {
            Optional<String> interactionId = extractInteractionId(httpHeaders);

            if (interactionId.isPresent()) {
                return performRequestAccordingToInteractionId(interactionId.get(), serverHttpRequest);
            }
        }
        return Optional.empty();
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
        List<String> interactionIds = httpHeaders.get(SSP_INTERACTION_ID);
        if (!CollectionUtils.isEmpty(interactionIds)) {
            return Optional.of(interactionIds.get(0));
        }

        return Optional.empty();
    }

    private Optional<URI> prepareLookupUri(String address, RequestPath requestPath) {
        if (StringUtils.isNotBlank(address)) {
            try {
                URI uri = new URI(address);
                String baseUri = uri.getScheme() + SCHEMA_SEPARATOR + uri.getHost();

                if (uri.getPort() != -1) {
                    baseUri += COLON + uri.getPort();
                }
                return Optional.of(new URI(baseUri + requestPath.value()));
            } catch (URISyntaxException e) {
                LOGGER.error("Invalid address in SDS: " + address);
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
