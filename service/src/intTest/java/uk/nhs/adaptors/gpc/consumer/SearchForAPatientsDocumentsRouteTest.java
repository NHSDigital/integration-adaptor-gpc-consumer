package uk.nhs.adaptors.gpc.consumer;

import static org.apache.commons.fileupload.FileUploadBase.CONTENT_TYPE;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SearchForAPatientsDocumentsRouteTest extends CloudGatewayRouteBaseTest {

    private static final String FHIR_QUERY = "fhir/Patient/2/DocumentReference?_include=DocumentReference"
        + ":subject:Patient&_include=DocumentReference:custodian:Organization&_include=DocumentReference:author:Organization&"
        + "_include=DocumentReference:author:Practitioner&_revinclude:recurse=PractitionerRole:practitioner";

    private static final String SDS_LOOKUP_FIND_PATIENT_DOCS_URI = SDS_LOOKUP_URI + FHIR_QUERY;
    private static final String EXAMPLE_MESSAGE_BODY = "{\"resourceType\":\"Bundle\","
        + "\"meta\":{\"profile\":[\"https://fhir.nhs.uk/STU3/StructureDefinition/GPConnect-Searchset-Bundle-1\"]},"
        + "\"type\":\"collection\",\"entry\":[]}";

    private static final String GPC_CONSUMER_FIND_PATIENT_DOCUMENT_URI_BASE = "/GP0001/STU3/1/gpconnect/documents/";
    private static final String GPC_CONSUMER_FIND_PATIENT_DOCUMENT_URI = GPC_CONSUMER_FIND_PATIENT_DOCUMENT_URI_BASE + FHIR_QUERY;
    private static final String DOCUMENT_SEARCH_ID = "urn:nhs:names:services:gpconnect:documents:fhir:rest:search:documentreference-1";

    @Test
    public void When_MakingRequestForStructuredDocument_Expect_OkResponse() {
        WIRE_MOCK_SERVER.stubFor(get(urlEqualTo(SDS_LOOKUP_FIND_PATIENT_DOCS_URI))
            .willReturn(aResponse()
                .withStatus(HttpStatus.SC_OK)
                .withHeader(CONTENT_TYPE, "application/fhir+json;charset=UTF-8")
                .withBody(EXAMPLE_MESSAGE_BODY)));

        WIRE_MOCK_SERVER.stubFor(get(urlPathEqualTo(ENDPOINT))
            .willReturn(aResponse()
                .withStatus(HttpStatus.SC_OK)
                .withBody(String.format(EXAMPLE_SDS_BODY, WIRE_MOCK_SERVER.baseUrl()))));

        getWebTestClient().get()
            .uri(GPC_CONSUMER_FIND_PATIENT_DOCUMENT_URI)
            .header(SSP_FROM_HEADER, ANY_STRING)
            .header(SSP_TO_HEADER, ANY_STRING)
            .header(SSP_INTERACTION_ID_HEADER, DOCUMENT_SEARCH_ID)
            .header(SSP_TRACE_ID_HEADER, ANY_STRING)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .json(EXAMPLE_MESSAGE_BODY);
    }

    @Test
    public void When_MakingRequestForFindPatientWithoutIdentifier_Expect_NotFoundResponse() {
        getWebTestClient().get()
            .uri(GPC_CONSUMER_FIND_PATIENT_DOCUMENT_URI)
            .exchange()
            .expectStatus()
            .isNotFound();
    }
}
