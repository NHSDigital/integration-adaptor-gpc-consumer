package uk.nhs.adaptors.gpc.consumer.sds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import java.util.List;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.util.TriFunction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ServerWebExchange;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;

import reactor.core.publisher.Mono;
import uk.nhs.adaptors.gpc.consumer.common.ResourceReader;
import uk.nhs.adaptors.gpc.consumer.sds.configuration.SdsConfiguration;
import uk.nhs.adaptors.gpc.consumer.sds.exception.SdsException;
import uk.nhs.adaptors.gpc.consumer.testcontainers.WiremockExtension;

@ExtendWith({SpringExtension.class, WiremockExtension.class})
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class SdsClientComponentTest {
    private static final String FROM_ODS_CODE = "ABC123";
    private static final String LOG_PREFIX = "Test log prefix";
    private static final String SSP_TRACE_ID = "Ssp-TraceID";
    private static final String X_CORRELATION_ID = String.valueOf(UUID.randomUUID());
    private static final String ADDRESS = "http://test/";

    private static final String GET_STRUCTURED_INTERACTION =
        "urn:nhs:names:services:gpconnect:fhir:operation:gpc.getstructuredrecord-1";
    private static final String PATIENT_SEARCH_ACCESS_DOCUMENT_INTERACTION =
        "urn:nhs:names:services:gpconnect:documents:fhir:rest:search:patient-1";
    private static final String SEARCH_FOR_DOCUMENT_INTERACTION =
        "urn:nhs:names:services:gpconnect:documents:fhir:rest:search:documentreference-1";
    private static final String RETRIEVE_DOCUMENT_INTERACTION =
        "urn:nhs:names:services:gpconnect:documents:fhir:rest:read:binary-1";

    @Autowired
    private WireMockServer wireMockServer;

    @Autowired
    private SdsConfiguration sdsConfiguration;

    @Autowired
    private SdsClient sdsClient;

    @Value("classpath:sds/sds_response.json")
    private Resource sdsResponse;

    @Value("classpath:sds/sds_no_result_response.json")
    private Resource sdsNoResultResponse;

    @Value("classpath:sds/sds_no_address_response.json")
    private Resource sdsNoAddressResponse;

    @Value("classpath:sds/sds_error_response.json")
    private Resource sdsErrorResponse;

    @Mock
    private static ServerWebExchange exchange;
    @Mock
    private static ServerHttpRequest request;
    @Mock
    private static HttpHeaders httpHeaders;

    private static List<Pair<String, TriFunction<String, String, ServerWebExchange, Mono<SdsClient.SdsResponseData>>>> allInteractions;

    @PostConstruct
    public void postConstruct() {
        allInteractions = List.of(
            Pair.of(GET_STRUCTURED_INTERACTION, sdsClient::callForGetStructuredRecord),
            Pair.of(PATIENT_SEARCH_ACCESS_DOCUMENT_INTERACTION, sdsClient::callForPatientSearchAccessDocument),
            Pair.of(SEARCH_FOR_DOCUMENT_INTERACTION, sdsClient::callForSearchForDocumentRecord),
            Pair.of(RETRIEVE_DOCUMENT_INTERACTION, sdsClient::callForRetrieveDocumentRecord));
    }

    @BeforeAll
    public static void beforeAll() {
        WireMock.configureFor(Integer.parseInt(System.getProperty("GPC_CONSUMER_WIREMOCK_PORT")));
    }

    @BeforeEach
    public void setUpMock() {
        when(exchange.getLogPrefix()).thenReturn(LOG_PREFIX);
        when(exchange.getRequest()).thenReturn(request);
        when(request.getHeaders()).thenReturn(httpHeaders);
        when(httpHeaders.getFirst(SSP_TRACE_ID)).thenReturn(SSP_TRACE_ID);
    }

    private void stubEndpoint(String interaction, String response) {
        stubFor(get(urlPathEqualTo("/Endpoint"))
                .withQueryParam("organization", equalTo("https://fhir.nhs.uk/Id/ods-organization-code|" + FROM_ODS_CODE))
                .withQueryParam("identifier", equalTo("https://fhir.nhs.uk/Id/nhsServiceInteractionId|" + interaction))
                .withHeader("apikey", matching(".*"))
                .withHeader("X-Correlation-Id", matching("[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}"))
            .willReturn(aResponse()
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(response)));
    }

    private void stubEndpointError() {
        stubFor(get(urlPathEqualTo("/Endpoint"))
            .willReturn(aResponse()
                .withStatus(HttpStatus.UNAUTHORIZED.value())
                .withHeader("Content-Type", "application/fhir+json")
                .withBody(ResourceReader.asString(sdsErrorResponse))));
    }

    @Test
    public void When_SdsReturnsResult_Expect_AddressIsReturned() {
        allInteractions.forEach(pair -> {
            wireMockServer.resetAll();
            stubEndpoint(pair.getKey(), ResourceReader.asString(sdsResponse));
            var retrievedSdsData = pair.getValue().apply(FROM_ODS_CODE, X_CORRELATION_ID, exchange).blockOptional();
            assertThat(retrievedSdsData)
                .isNotEmpty()
                .hasValue(SdsClient.SdsResponseData.builder().address(ADDRESS).build());
            wireMockServer.resetAll();
        });
    }

    @Test
    public void When_SdsReturnsNoResult_Expect_EmptyResultIsReturned() {
        allInteractions.forEach(pair -> {
            wireMockServer.resetAll();
            stubEndpoint(pair.getKey(), ResourceReader.asString(sdsNoResultResponse));
            assertThatThrownBy(() -> pair.getValue().apply(FROM_ODS_CODE, X_CORRELATION_ID, exchange).block())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("SDS returned no result");
            wireMockServer.resetAll();
        });
    }

    @Test
    public void When_SdsReturnsEmptyAddress_Expect_EmptyResultIsReturned() {
        allInteractions.forEach(pair -> {
            wireMockServer.resetAll();
            stubEndpoint(pair.getKey(), ResourceReader.asString(sdsNoAddressResponse));
            assertThatThrownBy(() -> pair.getValue().apply(FROM_ODS_CODE, X_CORRELATION_ID, exchange).block())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("SDS returned a result but with an empty address");
            wireMockServer.resetAll();
        });
    }

    @Test
    public void When_SdsReturnsError_Expect_Exception() {
        allInteractions.forEach(pair -> {
            wireMockServer.resetAll();
            stubEndpointError();
            assertThatThrownBy(() -> pair.getValue().apply(FROM_ODS_CODE, X_CORRELATION_ID, exchange).blockOptional())
                .isInstanceOf(SdsException.class);
            wireMockServer.resetAll();
        });
    }

    @Test
    public void When_NoXCorrelationIdPresent_Expect_Exception() {
        allInteractions.forEach(pair -> {
            wireMockServer.resetAll();
            stubEndpointError();
            assertThatThrownBy(() -> pair.getValue().apply(FROM_ODS_CODE, null, exchange).blockOptional())
                .isInstanceOf(SdsException.class);
            wireMockServer.resetAll();
        });
    }

    @Test
    public void When_InvalidXCorrelationId_Expect_Exception() {
        allInteractions.forEach(pair -> {
            wireMockServer.resetAll();
            stubEndpointError();
            assertThatThrownBy(() -> pair.getValue().apply(FROM_ODS_CODE, "not-UUID", exchange).blockOptional())
                .isInstanceOf(SdsException.class);
            wireMockServer.resetAll();
        });
    }
}
