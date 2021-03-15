package uk.nhs.adaptors.gpc.consumer;

import static org.apache.commons.fileupload.FileUploadBase.CONTENT_TYPE;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.util.DefaultUriBuilderFactory;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SearchPatientDocsRouteTest extends CloudGatewayRouteBaseTest {
    private static final String FIND_PATIENT_DOCS_URI = DOCUMENT_PATIENT_URI + "/2/DocumentReference?_include=DocumentReferen"
        + "ce%3Asubject%3APatient&_include=DocumentReference%3Acustodian%3AOrganization&_include=DocumentReference%3Aauthor%3AOrganization&"
        + "_include=DocumentReference%3Aauthor%3APractitioner&_revinclude%3Arecurse=PractitionerRole%3Apractitioner";
    private static final String EXAMPLE_MESSAGE_BODY = "{\"resourceType\":\"Bundle\","
        + "\"meta\":{\"profile\":[\"https://fhir.nhs.uk/STU3/StructureDefinition/GPConnect-Searchset-Bundle-1\"]},"
        + "\"type\":\"collection\",\"entry\":[]}";

    @Test
    public void When_MakingRequestForStructuredDocument_Expect_OkResponse() {
        WIRE_MOCK_SERVER.stubFor(get(urlEqualTo(FIND_PATIENT_DOCS_URI))
            .willReturn(aResponse()
                .withStatus(HttpStatus.SC_OK)
                .withHeader(CONTENT_TYPE, "application/fhir+json;charset=UTF-8")
                .withBody(EXAMPLE_MESSAGE_BODY)));
        WIRE_MOCK_SERVER.stubFor(get(urlPathEqualTo(ENDPOINT))
            .willReturn(aResponse()
                .withStatus(HttpStatus.SC_OK)
                .withBody(String.format(EXAMPLE_SDS_BODY, WIRE_MOCK_SERVER.baseUrl()))));

        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory(WIRE_MOCK_SERVER.baseUrl());
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

        getWebTestClient().get()
            .uri(factory.expand(FIND_PATIENT_DOCS_URI))
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .json(EXAMPLE_MESSAGE_BODY);
    }

    @Test
    public void When_MakingRequestForFindPatientWithoutIdentifier_Expect_NotFoundResponse() {
        getWebTestClient().get()
            .uri(FIND_PATIENT_DOCS_URI)
            .exchange()
            .expectStatus()
            .isNotFound();
    }
}
