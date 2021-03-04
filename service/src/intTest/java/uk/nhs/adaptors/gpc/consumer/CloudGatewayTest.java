package uk.nhs.adaptors.gpc.consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

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
    private static final WireMockServer WIRE_MOCK_SERVER = new WireMockServer(WIREMOCK_PORT);
    private static final String STRUCTURED_URI = "/GP0001/STU3/1/gpconnect/fhir/Patient/$gpc.getstructuredrecord";
    private static final String LOCALHOST_URI = "http://localhost:";
    private static final String ACTUATOR_ROUTES_URI = "/actuator/gateway/routes";
    private static final String EXAMPLE_STRUCTURED_BODY = "{\"resourceType\":\"Bundle\"," +
        "\"meta\":{\"profile\":[\"https://fhir.nhs" +
        ".uk/STU3/StructureDefinition/GPConnect-StructuredRecord-Bundle-1\"]}," +
        "\"type\":\"collection\",\"entry\":[]}";

    @LocalServerPort
    private int port = 0;

    private WebTestClient webTestClient;;
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
            .responseTimeout(Duration.ofSeconds(10))
            .baseUrl(baseUri)
            .build();
    }

    @AfterEach
    public void tearDown() {
        WIRE_MOCK_SERVER.resetAll();
    }

    @Test
    public void When_MakingRequestToActuatorRoutes_Expect_OkResponse() {
        webTestClient.get()
            .uri(baseUri + ACTUATOR_ROUTES_URI)
            .exchange()
            .expectStatus()
            .isOk();
    }

    @Test
    public void When_MakingRequestForStructuredDocument_Expect_OkResponse() {
        WIRE_MOCK_SERVER.stubFor(post(urlEqualTo(STRUCTURED_URI))
            .willReturn(aResponse()
                .withStatus(HttpStatus.SC_OK)
                .withBody(EXAMPLE_STRUCTURED_BODY)));

        webTestClient.post()
            .uri(baseUri + STRUCTURED_URI)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .json(EXAMPLE_STRUCTURED_BODY);
    }
}
