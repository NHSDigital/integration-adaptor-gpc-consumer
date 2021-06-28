package uk.nhs.adaptors.gpc.consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class FindAPatientRouteTest extends CloudGatewayRouteBaseTest {
    private static final String PATIENT_RESOURCE = "/%s/STU3/1/gpconnect/documents/fhir/Patient";
    private static final String QUERY_PARAMS = "?identifier=https://fhir.nhs.uk/Id/nhs-number|%s";
    private static final String REQUEST_URI_TEMPLATE = PATIENT_RESOURCE + QUERY_PARAMS;
    private static final String PATIENT_SEARCH_ID = "urn:nhs:names:services:gpconnect:documents:fhir:rest:search:patient-1";

    @ParameterizedTest(name = "{argumentsWithNames} {displayName}")
    @MethodSource(value = "uk.nhs.adaptors.gpc.consumer.Fixtures#orgCodes")
    public void When_FindAPatientResponseIsOk_Expect_ProxyResponseIsOkWithSameBody(String odsCode) {
        var nhsNumber = Fixtures.Patient.NO_DOCUMENTS.getNhsNumber();
        var requestUri = String.format(REQUEST_URI_TEMPLATE, odsCode, nhsNumber);
        getWebTestClientForStandardGet(requestUri, PATIENT_SEARCH_ID)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.type").isEqualTo("searchset")
            .jsonPath("$.entry[*].resource.identifier[*].value").isEqualTo(nhsNumber);
    }

    @Test
    public void When_SdsErrorOccursBeforeFindAPatient_Expect_ProxyResponseIsServerErrorOperationOutcome() {
        var org = Fixtures.Organization.MOCK_ORG.getOdsCode();
        var nhsNumber = Fixtures.Patient.NO_DOCUMENTS.getNhsNumber();
        var requestUri = String.format(REQUEST_URI_TEMPLATE, org, nhsNumber);

        When_GetRequestProducesSdsError_Expect_OperationOutcomeErrorResponse(requestUri, PATIENT_SEARCH_ID);
    }

    @Test
    public void When_FindAPatientResponseIsError_Expect_ProxyResponseIsErrorWithSameBody() {
        var org = Fixtures.Organization.MOCK_ORG.getOdsCode();
        var requestUri = String.format(REQUEST_URI_TEMPLATE, org, "NotNHSNumber");

        getWebTestClientForStandardGet(requestUri, PATIENT_SEARCH_ID)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody()
            .jsonPath("issue[*].details.coding[*].code").isEqualTo("INVALID_NHS_NUMBER");
    }
}
