package uk.nhs.adaptors.gpc.consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class MigrateDocumentRouteTest extends CloudGatewayRouteBaseTest {

    static final String MIGRATE_DOCUMENT_INTERACTION_ID = "urn:nhs:names:services:gpconnect:documents:fhir:rest:migrate:binary-1";
    private static final String REQUEST_URI_TEMPLATE = "/%s/STU3/1/gpconnect/fhir/Binary/%s";

    @ParameterizedTest(name = "{argumentsWithNames} {displayName}")
    @MethodSource(value = "uk.nhs.adaptors.gpc.consumer.Fixtures#orgCodes")
    public void When_MakingRequestForExistingDocument_Expect_OkResponse(String odsCode) {
        var binaryId = Fixtures.Binary.MSWORD.getId();
        var requestUri = String.format(REQUEST_URI_TEMPLATE, odsCode, binaryId);
        getWebTestClientForStandardGet(requestUri, MIGRATE_DOCUMENT_INTERACTION_ID)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.id").isEqualTo(binaryId);
    }

    @Test
    public void When_MakingRequestForNonExistingDocument_Expect_NotFoundResponse() {
        var odsCode = Fixtures.Organization.MOCK_ORG.getOdsCode();
        var requestUri = String.format(REQUEST_URI_TEMPLATE, odsCode, "not-found-id");
        getWebTestClientForStandardGet(requestUri, MIGRATE_DOCUMENT_INTERACTION_ID)
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectBody()
            .jsonPath("issue[*].details.coding[*].code").isEqualTo("REFERENCE_NOT_FOUND");
    }

    @Test
    public void When_SdsErrorOccurs_Expect_ProxyResponseIsServerErrorOperationOutcome() {
        var binaryId = Fixtures.Binary.MSWORD.getId();
        var odsCode = Fixtures.Organization.MOCK_ORG.getOdsCode();
        var requestUri = String.format(REQUEST_URI_TEMPLATE, odsCode, binaryId);

        When_GetRequestProducesSdsError_Expect_OperationOutcomeErrorResponse(requestUri, MIGRATE_DOCUMENT_INTERACTION_ID);
    }
}
