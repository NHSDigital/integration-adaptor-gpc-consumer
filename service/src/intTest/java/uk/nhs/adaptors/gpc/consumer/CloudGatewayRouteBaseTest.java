package uk.nhs.adaptors.gpc.consumer;

import java.time.Duration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.github.tomakehurst.wiremock.WireMockServer;

import lombok.Getter;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CloudGatewayRouteBaseTest {
    private static final int WIREMOCK_PORT = 8210;
    private static final int MAX_TIMEOUT = 10;
    private static final String LOCALHOST_URI = "http://localhost:";
    protected static final WireMockServer WIRE_MOCK_SERVER = new WireMockServer(WIREMOCK_PORT);
    protected static final String DOCUMENT_PATIENT_URI = "/GP0001/STU3/1/gpconnect/documents/Patient";
    protected static final String ENDPOINT = "/Endpoint";
    protected static final String SSP_FROM_HEADER = "Ssp-From";
    protected static final String SSP_TO_HEADER = "Ssp-To";
    protected static final String SSP_INTERACTION_ID_HEADER = "Ssp-InteractionID";
    protected static final String SSP_TRACE_ID_HEADER = "Ssp-TraceID";
    protected static final String ANY_STRING = "any";
    protected static final String EXAMPLE_SDS_BODY = "{\"resourceType\":\"Bundle\","
        + "\"id\":\"47DBB1CA-256D-410E-B00B-C19C1F13E9F6\","
        + "\"entry\":[{\"resource\":{\"resourceType\":\"Endpoint\",\"id\":\"307B4278-DFED-4A27-8B51-1539DB1B2C62\","
        + "\"address\":\"%s/GP0001/STU3/1/gpconnect/\"}}]}";
    protected static final String EXPECTED_NOT_FOUND_BODY = "{\"resourceType\": \"OperationOutcome\",\"meta\": {\"profile\": "
        + "[\"https://fhir.nhs.uk/StructureDefinition/gpconnect-operationoutcome-1\" ]},\"issue\": [{\"severity\": \"error\","
        + "\"code\": \"invalid\",\"details\": {\"coding\":[{\"system\": \"https://fhir.nhs.uk/ValueSet/gpconnect-error-or-warning-code-1\","
        + "\"code\": \"NO_RECORD_FOUND\",\"display\": \"No Record Found\"}]},\"diagnostics\": \"No record found\"}]}";

    @LocalServerPort
    private int port = 0;
    @Getter
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
}
