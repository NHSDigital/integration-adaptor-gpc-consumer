package uk.nhs.adaptors.gpc.consumer.sds.builder;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.netty.http.client.HttpClient;
import uk.nhs.adaptors.gpc.consumer.sds.configuration.SdsConfiguration;
import uk.nhs.adaptors.gpc.consumer.web.RequestBuilderService;
import uk.nhs.adaptors.gpc.consumer.web.WebClientFilterService;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class SdsRequestBuilder {

    private static final String PIPE = "|";
    private static final String ORG_CODE_PARAMETER = "organization";
    private static final String ORG_CODE_IDENTIFIER = "https://fhir.nhs.uk/Id/ods-organization-code";
    private static final String INTERACTION_PARAMETER = "identifier";
    private static final String MANUFACTURING_ORGANIZATION = "manufacturing-organization";
    private static final String INTERACTION_IDENTIFIER = "https://fhir.nhs.uk/Id/nhsServiceInteractionId";
    private static final String ENDPOINT_MHS_ENDPOINT = "/Endpoint";
    private static final String ENDPOINT_AS_DEVICE = "/Device";

    private static final String API_KEY_HEADER = "apikey";
    private static final String X_CORRELATION_ID_HEADER = "X-Correlation-Id";
    private final SdsConfiguration sdsConfiguration;
    private final RequestBuilderService requestBuilderService;
    private final WebClientFilterService webClientFilterService;

    @Value("${gpc-consumer.sds.supplierOdsCode}")
    private String supplierOdsCode;


    public RequestHeadersSpec<?> buildEndpointRequest(String fromOdsCode, String correlationId, String interactionUrn) {
        return buildMhsEndpointRequest(fromOdsCode, interactionUrn, correlationId);
    }

    public RequestHeadersSpec<?> buildDeviceRequest(String fromOdsCode, String correlationId, String interactionUrn) {
        return buildAsDeviceRequest(fromOdsCode, interactionUrn, correlationId);
    }

    private RequestHeadersSpec<? extends RequestHeadersSpec<?>> buildMhsEndpointRequest(String consumerOrgOdsCode,
                                                                                        String interaction, String correlationId) {
        return buildClientFor(consumerOrgOdsCode, interaction, correlationId, ENDPOINT_MHS_ENDPOINT);
    }

    private RequestHeadersSpec<? extends RequestHeadersSpec<?>> buildAsDeviceRequest(String odsCode,
                                                                                     String interaction, String correlationId) {
        return buildClientFor(odsCode, interaction, correlationId, ENDPOINT_AS_DEVICE);
    }


    @NotNull
    public RequestHeadersSpec<? extends RequestHeadersSpec<?>> buildAsDeviceAsidRequest(
            String consumerOrgOdsCode,
            String interaction,
            String correlationId
    ) {
        LOGGER.debug("Building ASID Device request (consumerOdsCode={}, supplierOdsCode={}, interaction={})",
            consumerOrgOdsCode, supplierOdsCode, interaction);

        var httpClient = getHttpClient();

        return buildWebClient(httpClient)
            .get()
            .uri(uriBuilder -> uriBuilder
                .path(ENDPOINT_AS_DEVICE)
                .queryParam(ORG_CODE_PARAMETER, ORG_CODE_IDENTIFIER + PIPE + consumerOrgOdsCode)
                .queryParam(INTERACTION_PARAMETER, INTERACTION_IDENTIFIER + PIPE + interaction)
                .queryParam(MANUFACTURING_ORGANIZATION, ORG_CODE_IDENTIFIER + PIPE + supplierOdsCode)
                .build())
            .header(API_KEY_HEADER, sdsConfiguration.getApiKey())
            .header(X_CORRELATION_ID_HEADER, correlationId);
    }

    @NotNull
    private RequestHeadersSpec<? extends RequestHeadersSpec<?>> buildClientFor(String odsCode, String interaction,
                                                                               String correlationId, String path) {
        LOGGER.debug("Building SDS request (odsCode={}, interaction={}, path={})", odsCode, interaction, path);
        var httpClient = getHttpClient();

        return buildWebClient(httpClient)
            .get()
            .uri(uriBuilder -> uriBuilder
                .path(path)
                .queryParam(ORG_CODE_PARAMETER, ORG_CODE_IDENTIFIER + PIPE + odsCode)
                .queryParam(INTERACTION_PARAMETER, INTERACTION_IDENTIFIER + PIPE + interaction)
                .build())
            .header(API_KEY_HEADER, sdsConfiguration.getApiKey())
            .header(X_CORRELATION_ID_HEADER, correlationId);
    }

    private WebClient buildWebClient(HttpClient httpClient) {
        return WebClient
            .builder()
            .exchangeStrategies(requestBuilderService.buildExchangeStrategies())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .filters(this::addWebClientFilters)
            .baseUrl(sdsConfiguration.getUrl())
            .build();
    }

    @NotNull
    private HttpClient getHttpClient() {
        var sslContext = requestBuilderService.buildStandardSslContext();
        return HttpClient.create().secure(t -> t.sslContext(sslContext));
    }

    private void addWebClientFilters(List<ExchangeFilterFunction> filters) {
        filters.add(webClientFilterService.logRequest());
        filters.add(webClientFilterService.errorHandlingFilter(WebClientFilterService.RequestType.SDS, HttpStatus.OK));
        filters.add(webClientFilterService.logResponse());
    }
}
