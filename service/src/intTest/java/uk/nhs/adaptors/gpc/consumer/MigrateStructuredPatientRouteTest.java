package uk.nhs.adaptors.gpc.consumer;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

public class MigrateStructuredPatientRouteTest extends CloudGatewayRouteBaseTest {
    static final String REQUEST_URI_TEMPLATE = "/%s/STU3/1/gpconnect/fhir/Patient/$gpc.migratestructuredrecord";
    static final String REQUEST_BODY_TEMPLATE = ResourceHelper.loadClasspathResourceAsString("/gpc/migrateStructuredPatient.json");
    static final String MIGRATE_STRUCTURED_INTERACTION_ID = "urn:nhs:names:services:gpconnect:fhir:operation:gpc.migratestructuredrecord-1";

    @ParameterizedTest(name = "{argumentsWithNames} {displayName}")
    @MethodSource(value = "uk.nhs.adaptors.gpc.consumer.Fixtures#orgCodes")
    public void When_MakingRequestForMigrateStructuredRecordPatient_Expect_OkResponse(String odsCode) {
        var requestUri = String.format(REQUEST_URI_TEMPLATE, odsCode);
        var nhsNumber = Fixtures.Patient.HAS_DOCUMENTS.getNhsNumber();
        var requestBody = String.format(REQUEST_BODY_TEMPLATE, nhsNumber);
        getWebTestClientForStandardPost(requestUri, MIGRATE_STRUCTURED_INTERACTION_ID)
            .header("Testing-X-Forward", "false")
            .bodyValue(requestBody)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.entry[*].resource.identifier[*].value").value(Matchers.hasItem(nhsNumber));
    }

    @Test
    public void When_MakingRequestForNonExistingMigrateStructuredRecordPatient_Expect_NotFoundResponse() {
        var odsCode = Fixtures.Organization.MOCK_ORG.getOdsCode();
        var requestUri = String.format(REQUEST_URI_TEMPLATE, odsCode);
        var requestBody = String.format(REQUEST_BODY_TEMPLATE, "1234567890");
        getWebTestClientForStandardPost(requestUri, MIGRATE_STRUCTURED_INTERACTION_ID)
            .header("Testing-X-Forward", "false")
            .bodyValue(requestBody)
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectBody()
            .jsonPath("issue[*].details.coding[*].code").isEqualTo("PATIENT_NOT_FOUND");
    }

    @Test
    public void When_SdsErrorOccursBeforeAccessMigrateStructuredPatient_Expect_ProxyResponseIsServerErrorOperationOutcome() {
        var odsCode = Fixtures.Organization.MOCK_ORG.getOdsCode();
        var requestUri = String.format(REQUEST_URI_TEMPLATE, odsCode);
        var nhsNumber = Fixtures.Patient.HAS_DOCUMENTS.getNhsNumber();
        var requestBody = String.format(REQUEST_BODY_TEMPLATE, nhsNumber);

        // Using Ssp-TraceID not a UUID to force an error from the SDS mock
        getWebTestClient().post().uri(requestUri)
            .bodyValue(requestBody)
            .header(SSP_FROM_HEADER, ANY_STRING)
            .header(SSP_TO_HEADER, ANY_STRING)
            .header(SSP_INTERACTION_ID_HEADER, MIGRATE_STRUCTURED_INTERACTION_ID)
            .header(SSP_TRACE_ID_HEADER, "NotUUID")
            .header(HttpHeaders.AUTHORIZATION, "anytoken")
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        // TODO: NIAD-1165 GPCC should use the SDS API OperationOutcome here instead of a Spring default error response body
    }
}
