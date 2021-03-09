package uk.nhs.adaptors.gpc.consumer;

import static org.apache.commons.fileupload.FileUploadBase.CONTENT_TYPE;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
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
import org.springframework.web.util.DefaultUriBuilderFactory;

import com.github.tomakehurst.wiremock.WireMockServer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class FindAPatientDocsGatewayFilterFactoryTest {
    private static final int WIREMOCK_PORT = 8210;
    private static final int MAX_TIMEOUT = 10;
    private static final WireMockServer WIRE_MOCK_SERVER = new WireMockServer(WIREMOCK_PORT);
    private static final String FIND_PATIENT_DOCS_URI = "/GP0001/STU3/1/gpconnect/fhir/Patient/2/DocumentReference?_include=DocumentReference%3Asubject%3APatient&_include=DocumentReference%3Acustodian%3AOrganization&_include=DocumentReference%3Aauthor%3AOrganization&_include=DocumentReference%3Aauthor%3APractitioner&_revinclude%3Arecurse=PractitionerRole%3Apractitioner";
    private static final String LOCALHOST_URI = "http://localhost:";
    private static final String EXAMPLE_MESSAGE_BODY = "{\"resourceType\":\"Bundle\","
        + "\"meta\":{\"profile\":[\"https://fhir.nhs.uk/STU3/StructureDefinition/GPConnect-Searchset-Bundle-1\"]},"
        + "\"type\":\"collection\",\"entry\":[]}";

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
        WIRE_MOCK_SERVER.stubFor(get(urlEqualTo(FIND_PATIENT_DOCS_URI))
            .willReturn(aResponse()
                .withStatus(HttpStatus.SC_OK)
                .withHeader(CONTENT_TYPE, "application/fhir+json;charset=UTF-8")
                .withBody(EXAMPLE_MESSAGE_BODY)));

        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(WIRE_MOCK_SERVER.baseUrl());
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

        webTestClient.get()
            .uri(factory.expand(FIND_PATIENT_DOCS_URI))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .json(EXAMPLE_MESSAGE_BODY);
    }

    @Test
    public void When_MakingRequestForFindPatientWithoutIdentifier_Expect_NotFoundResponse() {
        webTestClient.get()
            .uri(FIND_PATIENT_DOCS_URI)
            .exchange()
            .expectStatus()
            .isNotFound();
    }
}
