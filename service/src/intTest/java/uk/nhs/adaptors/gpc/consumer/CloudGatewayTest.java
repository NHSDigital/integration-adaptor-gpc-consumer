package uk.nhs.adaptors.gpc.consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import java.time.Duration;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.github.tomakehurst.wiremock.WireMockServer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CloudGatewayTest {
    private static final int WIREMOCK_PORT = 8210;
    private static final int MAX_TIMEOUT = 10;
    private static final WireMockServer WIRE_MOCK_SERVER = new WireMockServer(WIREMOCK_PORT);
    private static final String STRUCTURED_URI = "/GP0001/STU3/1/gpconnect/fhir/Patient/$gpc.getstructuredrecord";
    private static final String LOCALHOST_URI = "http://localhost:";
    private static final String EXAMPLE_STRUCTURED_BODY = "{\"resourceType\":\"Bundle\","
        + "\"meta\":{\"profile\":[\"https://fhir.nhs"
        + ".uk/STU3/StructureDefinition/GPConnect-StructuredRecord-Bundle-1\"]},"
        + "\"type\":\"collection\",\"entry\":[]}";
    private static final String GET_DOCUMENT_URI = "/GP0001/STU3/1/gpconnect/fhir/Binary/07a6483f-732b-461e-86b6-edb665c45510";
    private static final String EXPECTED_DOCUMENT_BODY = "{\"resourceType\": \"Binary\","
        + "\"id\": \"07a6483f-732b-461e-86b6-edb665c45510\","
        + "\"contentType\": \"application/msword\","
        + "\"content\": \"response content\"}";
    private static final String NOT_FOUND_GET_DOCUMENT_URI = "/GP0001/STU3/1/gpconnect/fhir/Binary/00000000-732b-461e-86b6-edb665c45510";
    private static final String EXPECTED_NOT_FOUND_BODY = "{\"resourceType\": \"OperationOutcome\",\"meta\": {\"profile\": "
        + "[\"https://fhir.nhs.uk/StructureDefinition/gpconnect-operationoutcome-1\" ]},\"issue\": [{\"severity\": \"error\","
        + "\"code\": \"invalid\",\"details\": {\"coding\":[{\"system\": \"https://fhir.nhs.uk/ValueSet/gpconnect-error-or-warning-code-1\","
        + "\"code\": \"NO_RECORD_FOUND\",\"display\": \"No Record Found\"}]},\"diagnostics\": \"No record found\"}]}";
    private static final String SSP_FROM_HEADER = "Ssp-From";
    private static final String SSP_TO_HEADER = "Ssp-To";
    private static final String SSP_INTERACTION_ID_HEADER = "Ssp-InteractionID";
    private static final String SSP_TRACE_ID_HEADER = "Ssp-TraceID";
    private static final String DOCUMENT_INTERACTION_ID = "urn:nhs:names:services:gpconnect:documents:fhir:rest:read:binary-1";
    private static final String ANY_STRING = "any";

    @LocalServerPort
    private int port = 0;

    private WebTestClient webTestClient;

    private String baseUri;

    @BeforeAll
    public static void initialize() {
        WIRE_MOCK_SERVER.start();
    }

    @AfterAll
    public static void deinitialize() {
        WIRE_MOCK_SERVER.stop();
    }

    @BeforeEach
    public void setUp() {
        baseUri = LOCALHOST_URI + port;
        webTestClient = WebTestClient.bindToServer()
            .responseTimeout(Duration.ofSeconds(MAX_TIMEOUT))
            .baseUrl(baseUri)
            .build();
    }

    @AfterEach
    public void tearDown() {
        WIRE_MOCK_SERVER.resetAll();
    }

    @Test
    public void When_MakingRequestForStructuredDocument_Expect_OkResponse() {
        WIRE_MOCK_SERVER.stubFor(post(urlPathEqualTo(STRUCTURED_URI))
            .willReturn(aResponse()
                .withStatus(HttpStatus.SC_OK)
                .withBody(EXAMPLE_STRUCTURED_BODY)));

        webTestClient.post()
            .uri(STRUCTURED_URI)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .json(EXAMPLE_STRUCTURED_BODY);
    }

    @Test
    public void When_MakingRequestForSpecificDocument_Expect_OkResponse() {
        WIRE_MOCK_SERVER.stubFor(get(urlPathEqualTo(GET_DOCUMENT_URI))
            .willReturn(aResponse()
                .withStatus(HttpStatus.SC_OK)
                .withBody(EXPECTED_DOCUMENT_BODY)));

        webTestClient.get()
            .uri(GET_DOCUMENT_URI)
            .header(SSP_FROM_HEADER, ANY_STRING)
            .header(SSP_TO_HEADER, ANY_STRING)
            .header(SSP_INTERACTION_ID_HEADER, DOCUMENT_INTERACTION_ID)
            .header(SSP_TRACE_ID_HEADER, ANY_STRING)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .json(EXPECTED_DOCUMENT_BODY);
    }

    @Test
    public void When_MakingRequestForSpecificDocument_Expect_ErrorResponse() {
        WIRE_MOCK_SERVER.stubFor(get(urlPathEqualTo(NOT_FOUND_GET_DOCUMENT_URI))
            .willReturn(aResponse()
                .withStatus(HttpStatus.SC_NOT_FOUND)
                .withBody(EXPECTED_NOT_FOUND_BODY)));

        webTestClient.get()
            .uri(NOT_FOUND_GET_DOCUMENT_URI)
            .header(SSP_FROM_HEADER, ANY_STRING)
            .header(SSP_TO_HEADER, ANY_STRING)
            .header(SSP_INTERACTION_ID_HEADER, DOCUMENT_INTERACTION_ID)
            .header(SSP_TRACE_ID_HEADER, ANY_STRING)
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectBody()
            .json(EXPECTED_NOT_FOUND_BODY);
    }
}
