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
    protected static final String FHIR_PATIENT_URI = "/GP0001/STU3/1/gpconnect/fhir/Patient";

    @LocalServerPort
    private int port = 0;
    @Getter
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
            .responseTimeout(Duration.ofSeconds(MAX_TIMEOUT))
            .baseUrl(baseUri)
            .build();
    }

    @AfterEach
    public void tearDown() {
        WIRE_MOCK_SERVER.resetAll();
    }
}
