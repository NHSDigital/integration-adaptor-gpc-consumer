package uk.nhs.adaptors.gpc.consumer.gpc;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.RouteToRequestUrlFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import uk.nhs.adaptors.gpc.consumer.filters.SdsFilter;
import uk.nhs.adaptors.gpc.consumer.sds.SdsClient;
import uk.nhs.adaptors.gpc.consumer.sds.exception.SdsException;

import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import reactor.test.StepVerifier;
import static uk.nhs.adaptors.gpc.consumer.filters.SdsFilter.SSP_INTERACTION_ID;
import static uk.nhs.adaptors.gpc.consumer.gpc.InteractionIds.DOCUMENT_READ_ID;
import static uk.nhs.adaptors.gpc.consumer.gpc.InteractionIds.STRUCTURED_ID;
import static uk.nhs.adaptors.gpc.consumer.utils.HeaderConstants.SSP_TRACE_ID;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class SdsFilterTest {

    @Mock
    private GatewayFilterChain chain;

    @Mock
    private SdsClient sdsClient;
    private SdsFilter sdsFilter;
    private GatewayFilterChain filterChain;
    private ArgumentCaptor<ServerWebExchange> captor;

    private static final String MIGRATE_STRUCTURED_INTERACTION
                                                    = "urn:nhs:names:services:gpconnect:fhir:operation:gpc.migratestructuredrecord-1";
    private static final String TEST_ODS_CODE = "A12345";
    private static final String TEST_TRACE_ID = "trace-id-123";
    private static final String TEST_URL = "/A12345/Patient/1";
    private static final String TEST_ADDRESS = "http://gpc-provider.example.com/fhir";
    private static final String TEST_ASID = "928940000057";
    private static final String TEST_CONSUMER_ASID = "928942012545";

    private MockServerWebExchange exchange;

    @BeforeEach
    @SneakyThrows
    public void before() {
        sdsFilter = new SdsFilter(sdsClient);
        sdsFilter.initializeSdsRequestFunctions();
        exchange = buildExchange(TEST_URL);

        filterChain = Mockito.mock(GatewayFilterChain.class);
        captor = ArgumentCaptor.forClass(ServerWebExchange.class);
    }

    @Test
    @SneakyThrows
    void When_NoSspHeaders_Expect_FetchValuesFromSds() {

        String odsCode = "A12345";
        String correlationId = "98765";
        String gpConnectServerAsid = "928940000057";
        String gpConnectConsumerAsid = "928942012545";

        MockServerHttpRequest request
                        = MockServerHttpRequest.get("/A12345/STU3/1/gpconnect/fhir/Patient/$gpc.migratestructuredrecord")
                        .header("Ssp-TraceID", correlationId)
                        .header("Ssp-InteractionID",
                                "urn:nhs:names:services:gpconnect:fhir:operation:gpc.migratestructuredrecord-1").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(sdsClient.callForGetAsid(MIGRATE_STRUCTURED_INTERACTION, odsCode, correlationId)).thenReturn(Mono.just(gpConnectConsumerAsid));
        when(sdsClient.callForMigrateStructuredRecord(odsCode, correlationId))
                                    .thenReturn(Mono.just(SdsClient.SdsResponseData.builder().nhsSpineAsid(gpConnectServerAsid).build()));
        when(filterChain.filter(captor.capture())).thenReturn(Mono.empty());

        sdsFilter.filter(exchange, filterChain).block();

        var resultExchange = captor.getValue();
        assertEquals(gpConnectConsumerAsid, resultExchange.getRequest().getHeaders().get("ssp-From").get(0));
        assertEquals(gpConnectServerAsid, resultExchange.getRequest().getHeaders().get("ssp-To").get(0));
    }

    @Test
    @SneakyThrows
    void When_SspValuesHeadersPresent_Expect_FetchSspValuesFromRequestHeaders() {

        String odsCode = "A12345";
        String correlationId = "98765";
        String gpConnectServerAsid = "928940000001";
        String gpConnectConsumerAsid = "928940000005";

        MockServerHttpRequest request
            = MockServerHttpRequest.get("/A12345/STU3/1/gpconnect/fhir/Patient/$gpc.migratestructuredrecord")
            .header("Ssp-TraceID", correlationId)
            .header("Ssp-InteractionID",
                    "urn:nhs:names:services:gpconnect:fhir:operation:gpc.migratestructuredrecord-1")
            .header("Ssp-To", gpConnectServerAsid)
            .header("Ssp-From", gpConnectConsumerAsid)
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(sdsClient.callForGetAsid(MIGRATE_STRUCTURED_INTERACTION, odsCode, correlationId)).thenReturn(Mono.just("928942012545"));
        when(sdsClient.callForMigrateStructuredRecord(odsCode, correlationId))
            .thenReturn(Mono.just(SdsClient.SdsResponseData.builder().nhsSpineAsid("928940000057").build()));
        when(filterChain.filter(captor.capture())).thenReturn(Mono.empty());

        sdsFilter.filter(exchange, filterChain).block();

        var resultExchange = captor.getValue();
        assertEquals(gpConnectConsumerAsid, resultExchange.getRequest().getHeaders().get("ssp-From").get(0));
        assertEquals(gpConnectServerAsid, resultExchange.getRequest().getHeaders().get("ssp-To").get(0));
    }

    @Test
    void shouldReturn404AndOperationOutcomeWhenPatientNotFound() {
        var exception = WebClientResponseException.create(
            HttpStatus.NOT_FOUND.value(), "Not Found", HttpHeaders.EMPTY, new byte[0], null);
        when(sdsClient.callForGetStructuredRecord(TEST_ODS_CODE, TEST_TRACE_ID))
            .thenReturn(Mono.error(exception));

        StepVerifier.create(sdsFilter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertOperationOutcomeBody(exchange, SdsFilter.PATIENT_NOT_FOUND, SdsFilter.NOT_FOUND);
    }

    @Test
    void shouldReturn400AndOperationOutcomeWhenBadRequest() {
        var exception = WebClientResponseException.create(
            HttpStatus.BAD_REQUEST.value(), "Bad Request", HttpHeaders.EMPTY, new byte[0], null);

        when(sdsClient.callForGetStructuredRecord(TEST_ODS_CODE, TEST_TRACE_ID))
            .thenReturn(Mono.error(exception));

        StepVerifier.create(sdsFilter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertOperationOutcomeBody(exchange, SdsFilter.BAD_REQUEST, SdsFilter.STRUCTURE);
    }

    @Test
    void shouldReturn502AndOperationOutcomeWhenUpstreamBadGateway() {
        var exception = WebClientResponseException.create(
            HttpStatus.BAD_GATEWAY.value(), "Bad Gateway", HttpHeaders.EMPTY, new byte[0], null);

        when(sdsClient.callForGetStructuredRecord(TEST_ODS_CODE, TEST_TRACE_ID))
            .thenReturn(Mono.error(exception));

        StepVerifier.create(sdsFilter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertOperationOutcomeBody(exchange, SdsFilter.BAD_GATEWAY, SdsFilter.EXCEPTION);
    }

    @Test
    void shouldReturn500AndOperationOutcomeWhenUnexpectedExceptionThrown() {
        when(sdsClient.callForGetStructuredRecord(TEST_ODS_CODE, TEST_TRACE_ID))
            .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

        StepVerifier.create(sdsFilter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertOperationOutcomeBody(exchange, SdsFilter.INTERNAL_SERVER_ERROR, SdsFilter.EXCEPTION);
    }

    @Test
    void shouldReturn502WhenWebClientRequestExceptionOccurs() {
        var exception = new WebClientRequestException(
            new RuntimeException("Connection refused"), org.springframework.http.HttpMethod.GET,
            URI.create("http://sds.example.com"), HttpHeaders.EMPTY);

        when(sdsClient.callForGetStructuredRecord(TEST_ODS_CODE, TEST_TRACE_ID))
            .thenReturn(Mono.error(exception));

        StepVerifier.create(sdsFilter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertOperationOutcomeBody(exchange, SdsFilter.INTERNAL_SERVER_ERROR, SdsFilter.EXCEPTION);
    }

    @Test
    void shouldReturn500WhenNoEndpointFoundInSds() {
        when(sdsClient.callForGetStructuredRecord(TEST_ODS_CODE, TEST_TRACE_ID))
            .thenReturn(Mono.empty());

        StepVerifier.create(sdsFilter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertOperationOutcomeBody(exchange, SdsFilter.INTERNAL_SERVER_ERROR, SdsFilter.EXCEPTION);
    }

    @Test
    void shouldReturn500WhenSdsExceptionThrown() {
        when(sdsClient.callForGetStructuredRecord(TEST_ODS_CODE, TEST_TRACE_ID))
            .thenReturn(Mono.error(new SdsException("SDS unavailable")));

        StepVerifier.create(sdsFilter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertOperationOutcomeBody(exchange, SdsFilter.INTERNAL_SERVER_ERROR, SdsFilter.EXCEPTION);
    }

    @Test
    void shouldReturnErrorWhenFhirPathContainsNeitherPatientNorBinary() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/A12345/Observation/1")
            .header(SSP_INTERACTION_ID, STRUCTURED_ID)
            .header(SSP_TRACE_ID, TEST_TRACE_ID)
            .build();
        MockServerWebExchange unsupportedPathExchange = MockServerWebExchange.from(request);

        when(sdsClient.callForGetStructuredRecord(TEST_ODS_CODE, TEST_TRACE_ID))
            .thenReturn(Mono.just(SdsClient.SdsResponseData.builder()
                .address(TEST_ADDRESS).nhsSpineAsid(TEST_ASID).build()));

        StepVerifier.create(sdsFilter.filter(unsupportedPathExchange, chain)).verifyComplete();

        assertThat(unsupportedPathExchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void shouldReturn503WhenWebClientResponseExceptionWithUnmappedStatus() {
        var exception = WebClientResponseException.create(
            HttpStatus.SERVICE_UNAVAILABLE.value(), "Service Unavailable", HttpHeaders.EMPTY, new byte[0], null);

        when(sdsClient.callForGetStructuredRecord(TEST_ODS_CODE, TEST_TRACE_ID))
            .thenReturn(Mono.error(exception));

        StepVerifier.create(sdsFilter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertOperationOutcomeBody(exchange, SdsFilter.INTERNAL_SERVER_ERROR, SdsFilter.EXCEPTION);
    }

    @Test
    void shouldSetGatewayRequestUrlAttributeWhenEndpointFoundWithPatientPath() {
        when(sdsClient.callForGetStructuredRecord(TEST_ODS_CODE, TEST_TRACE_ID))
            .thenReturn(Mono.just(SdsClient.SdsResponseData.builder()
                .address(TEST_ADDRESS).nhsSpineAsid(TEST_ASID).build()));
        when(sdsClient.callForGetAsid(STRUCTURED_ID, TEST_ODS_CODE, TEST_TRACE_ID))
            .thenReturn(Mono.just(TEST_CONSUMER_ASID));
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(sdsFilter.filter(exchange, chain)).verifyComplete();

        URI gatewayUrl = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        assertThat(gatewayUrl).isNotNull();
        assertThat(gatewayUrl.toString()).contains(TEST_ADDRESS);
        assertThat(gatewayUrl.toString()).contains("Patient");
    }

    @Test
    void shouldSetGatewayRequestUrlAttributeWhenEndpointFoundWithBinaryPath() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/A12345/STU3/1/gpconnect/fhir/Binary/doc-1")
            .header(SSP_INTERACTION_ID, DOCUMENT_READ_ID)
            .header(SSP_TRACE_ID, TEST_TRACE_ID)
            .build();
        MockServerWebExchange binaryExchange = MockServerWebExchange.from(request);

        when(sdsClient.callForRetrieveDocumentRecord(TEST_ODS_CODE, TEST_TRACE_ID))
            .thenReturn(Mono.just(SdsClient.SdsResponseData.builder()
                .address(TEST_ADDRESS).nhsSpineAsid(TEST_ASID).build()));
        when(sdsClient.callForGetAsid(DOCUMENT_READ_ID, TEST_ODS_CODE, TEST_TRACE_ID))
            .thenReturn(Mono.just(TEST_CONSUMER_ASID));
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(sdsFilter.filter(binaryExchange, chain)).verifyComplete();

        URI gatewayUrl = binaryExchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        assertThat(gatewayUrl).isNotNull();
        assertThat(gatewayUrl.toString()).contains(TEST_ADDRESS);
        assertThat(gatewayUrl.toString()).contains("Binary");
    }

    @Test
    void shouldPreserveQueryParametersInConstructedUri() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/A12345/STU3/1/gpconnect/fhir/Patient/1?_revinclude=Appointment")
            .header(SSP_INTERACTION_ID, STRUCTURED_ID)
            .header(SSP_TRACE_ID, TEST_TRACE_ID)
            .build();
        MockServerWebExchange queryExchange = MockServerWebExchange.from(request);

        when(sdsClient.callForGetStructuredRecord(TEST_ODS_CODE, TEST_TRACE_ID))
            .thenReturn(Mono.just(SdsClient.SdsResponseData.builder()
                .address(TEST_ADDRESS).nhsSpineAsid(TEST_ASID).build()));
        when(sdsClient.callForGetAsid(STRUCTURED_ID, TEST_ODS_CODE, TEST_TRACE_ID))
            .thenReturn(Mono.just(TEST_CONSUMER_ASID));
        when(chain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(sdsFilter.filter(queryExchange, chain)).verifyComplete();

        URI gatewayUrl = queryExchange.getAttribute(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
        assertThat(gatewayUrl).isNotNull();
        assertThat(gatewayUrl.toString()).contains("_revinclude=Appointment");
    }

    @Test
    void shouldReturnCorrectFilterOrder() {
        assertThat(sdsFilter.getOrder()).isEqualTo(RouteToRequestUrlFilter.ROUTE_TO_URL_FILTER_ORDER + 1);
    }

    @Test
    void shouldSetContentTypeToJsonInErrorResponse() {
        var exception = WebClientResponseException.create(
            HttpStatus.BAD_REQUEST.value(), "Bad Request", HttpHeaders.EMPTY, new byte[0], null);

        when(sdsClient.callForGetStructuredRecord(TEST_ODS_CODE, TEST_TRACE_ID))
            .thenReturn(Mono.error(exception));

        StepVerifier.create(sdsFilter.filter(exchange, chain)).verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
    }

    private MockServerWebExchange buildExchange(String path) {
        MockServerHttpRequest request = MockServerHttpRequest.get(path)
            .header(SSP_INTERACTION_ID, STRUCTURED_ID)
            .header(SSP_TRACE_ID, TEST_TRACE_ID)
            .build();
        return MockServerWebExchange.from(request);
    }

    private void assertOperationOutcomeBody(MockServerWebExchange exchange, String expectedSpineCode, String expectedFhirCode) {
        String body = exchange.getResponse().getBodyAsString().block();
        assertThat(body).contains("OperationOutcome");
        assertThat(body).contains(expectedSpineCode);
        assertThat(body).contains(expectedFhirCode);
    }

}
