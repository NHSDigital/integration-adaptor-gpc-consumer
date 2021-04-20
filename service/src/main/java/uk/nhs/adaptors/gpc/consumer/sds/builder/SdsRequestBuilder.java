package uk.nhs.adaptors.gpc.consumer.sds.builder;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

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
    private static final String INTERACTION_IDENTIFIER = "https://fhir.nhs.uk/Id/nhsServiceInteractionId";
    private static final String ENDPOINT = "/Endpoint";

    private static final String GET_STRUCTURED_INTERACTION =
        "urn:nhs:names:services:gpconnect:fhir:operation:gpc.getstructuredrecord-1";
    private static final String PATIENT_SEARCH_ACCESS_DOCUMENT_INTERACTION =
        "urn:nhs:names:services:gpconnect:documents:fhir:rest:search:patient-1";
    private static final String SEARCH_FOR_DOCUMENT_INTERACTION =
        "urn:nhs:names:services:gpconnect:documents:fhir:rest:search:documentreference-1";
    private static final String RETRIEVE_DOCUMENT_INTERACTION =
        "urn:nhs:names:services:gpconnect:documents:fhir:rest:read:binary-1";

    private static final String API_KEY_HEADER = "apikey";
    private static final String X_CORRELATION_ID_HEADER = "X-Correlation-Id";
    private final SdsConfiguration sdsConfiguration;
    private final RequestBuilderService requestBuilderService;
    private final WebClientFilterService webClientFilterService;

    public WebClient.RequestHeadersSpec<?> buildGetStructuredRecordRequest(String fromOdsCode, String correlationId) {
        return buildRequest(fromOdsCode, GET_STRUCTURED_INTERACTION, correlationId);
    }

    public WebClient.RequestHeadersSpec<?> buildPatientSearchAccessDocumentRequest(String fromOdsCode, String correlationId) {
        return buildRequest(fromOdsCode, PATIENT_SEARCH_ACCESS_DOCUMENT_INTERACTION, correlationId);
    }

    public WebClient.RequestHeadersSpec<?> buildSearchForDocumentRequest(String fromOdsCode, String correlationId) {
        return buildRequest(fromOdsCode, SEARCH_FOR_DOCUMENT_INTERACTION, correlationId);
    }

    public WebClient.RequestHeadersSpec<?> buildRetrieveDocumentRequest(String fromOdsCode, String correlationId) {
        return buildRequest(fromOdsCode, RETRIEVE_DOCUMENT_INTERACTION, correlationId);
    }

    private WebClient.RequestHeadersSpec<? extends WebClient.RequestHeadersSpec<?>> buildRequest(String odsCode, String interaction,
        String correlationId) {
        var sslContext = requestBuilderService.buildStandardSslContext();
        var httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));
        return buildWebClient(httpClient)
            .get()
            .uri(uriBuilder -> uriBuilder
                .path(ENDPOINT)
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

    private void addWebClientFilters(List<ExchangeFilterFunction> filters) {
        filters.add(webClientFilterService.errorHandlingFilter(WebClientFilterService.RequestType.SDS, HttpStatus.OK));
        filters.add(webClientFilterService.logRequest());
    }
}
