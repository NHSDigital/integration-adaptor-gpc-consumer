package uk.nhs.adaptors.gpc.consumer;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import static uk.nhs.adaptors.gpc.consumer.Fixtures.Organization.MOCK_ORG;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class SearchForAPatientsDocumentsRouteTest extends CloudGatewayRouteBaseTest {
    static final String REQUEST_URI_TEMPLATE = "/%s/STU3/1/gpconnect/documents/fhir/Patient/%s"
        + "/DocumentReference?_include=DocumentReference:subject:Patient"
        + "&_include=DocumentReference:custodian:Organization"
        + "&_include=DocumentReference:author:Organization"
        + "&_include=DocumentReference:author:Practitioner"
        + "&_revinclude:recurse=PractitionerRole:practitioner";
    static final String DOCUMENT_SEARCH_ID = "urn:nhs:names:services:gpconnect:documents:fhir:rest:search:documentreference-1";

    @ParameterizedTest(name = "{argumentsWithNames} {displayName}")
    @MethodSource(value = "uk.nhs.adaptors.gpc.consumer.Fixtures#orgCodes")
    public void When_PatientHasDocuments_Expect_OkResponseSearchsetWithDocumentReference(String odsCode) {
        var patientId = Fixtures.Patient.HAS_DOCUMENTS.getAccessDocumentLogicalId();
        var requestUri = String.format(REQUEST_URI_TEMPLATE, odsCode, patientId);
        var body = getWebTestClientForStandardGet(requestUri, DOCUMENT_SEARCH_ID)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.type").isEqualTo("searchset")
            .jsonPath("$.entry[*].resource.resourceType").value(hasItem("DocumentReference"))
            .jsonPath("$.entry[*].resource.content[*].attachment.url").value(hasItem(startsWith(getGpccAdaptorBaseUri())))
            .returnResult()
            .getResponseBody();
        System.out.println(new String(body));
    }

    @Test
    public void When_PatientHasNoDocuments_Expect_OkResponseEmptySearchsetWithoutDocumentReference() {
        var patientId = Fixtures.Patient.NO_DOCUMENTS.getAccessDocumentLogicalId();
        var odsCode = MOCK_ORG.getOdsCode();
        var requestUri = String.format(REQUEST_URI_TEMPLATE, odsCode, patientId);

        getWebTestClientForStandardGet(requestUri, DOCUMENT_SEARCH_ID)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.type").isEqualTo("searchset")
            .jsonPath("$.entry[*].resource.resourceType").value(not(hasItem("DocumentReference")));
    }

    @Test
    public void When_PatientDoesNotExist_Expect_NotFoundResponse() {
        var patientId = "notfound";
        var odsCode = MOCK_ORG.getOdsCode();
        var requestUri = String.format(REQUEST_URI_TEMPLATE, odsCode, patientId);
        getWebTestClientForStandardGet(requestUri, DOCUMENT_SEARCH_ID)
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectBody()
            .jsonPath("issue[*].details.coding[*].code").isEqualTo("PATIENT_NOT_FOUND");
    }

}
