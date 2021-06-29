package uk.nhs.adaptors.gpc.consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

public class RetrieveADocumentRouteTest extends CloudGatewayRouteBaseTest {

    static final String DOCUMENT_INTERACTION_ID = "urn:nhs:names:services:gpconnect:documents:fhir:rest:read:binary-1";
    private static final String REQUEST_URI_TEMPLATE = "/%s/STU3/1/gpconnect/documents/fhir/Binary/%s";

    @ParameterizedTest(name = "{argumentsWithNames} {displayName}")
    @MethodSource(value = "uk.nhs.adaptors.gpc.consumer.Fixtures#orgCodes")
    public void When_MakingRequestForSpecificDocument_Expect_OkResponse(String odsCode) {
        var binaryId = Fixtures.Binary.MSWORD.getId();
        var requestUri = String.format(REQUEST_URI_TEMPLATE, odsCode, binaryId);
        getWebTestClientForStandardGet(requestUri, DOCUMENT_INTERACTION_ID)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.id").isEqualTo(binaryId);
    }

    @Test
    public void When_MakingRequestForNotFoundDocument_Expect_ErrorResponse() {
        var odsCode = Fixtures.Organization.MOCK_ORG.getOdsCode();
        var requestUri = String.format(REQUEST_URI_TEMPLATE, odsCode, "not-found-id");
        getWebTestClientForStandardGet(requestUri, DOCUMENT_INTERACTION_ID)
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectBody()
            .jsonPath("issue[*].details.coding[*].code").isEqualTo("REFERENCE_NOT_FOUND");
    }

    @Test
    public void When_SdsErrorOccursBeforeRetrieveADocument_Expect_ProxyResponseIsServerErrorOperationOutcome() {
        var binaryId = Fixtures.Binary.MSWORD.getId();
        var odsCode = Fixtures.Organization.MOCK_ORG.getOdsCode();
        var requestUri = String.format(REQUEST_URI_TEMPLATE, odsCode, binaryId);

        When_GetRequestProducesSdsError_Expect_OperationOutcomeErrorResponse(requestUri, DOCUMENT_INTERACTION_ID);
    }
}
