package uk.nhs.adaptors.gpc.consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

public class GetDocumentTest extends CloudGatewayRouteBaseTest {
    private static final String GET_DOCUMENT_URI = "/GP0001/STU3/1/gpconnect/fhir/Binary/07a6483f-732b-461e-86b6-edb665c45510";
    private static final String EXPECTED_DOCUMENT_BODY = "{\"resourceType\": \"Binary\","
        + "\"id\": \"07a6483f-732b-461e-86b6-edb665c45510\","
        + "\"contentType\": \"application/msword\","
        + "\"content\": \"response content\"}";
    private static final String NOT_FOUND_GET_DOCUMENT_URI = "/GP0001/STU3/1/gpconnect/fhir/Binary/00000000-732b-461e-86b6-edb665c45510";
    private static final String EXPECTED_NOT_FOUND_BODY = "{\"resourceType\": \"OperationOutcome\",\"meta\": {\"profile\": "
        + "[\"https://fhir.nhs.uk/StructureDefinition/gpconnect-operationoutcome-1\" ]},\"issue\": [{\"severity\": \"error\","
        + "\"code\": \"invalid\",\"details\": {\"coding\":[{\"system\": \"https://fhir.nhs.uk/ValueSet/gpconnect-error-or-warning-code-1\","
        + "\"code\": \"NO_RECORD_FOUND\",\"display\": \"No Record Found\"}]},\"diagnostics\": \"No record found\"}]}";
    private static final String SSP_FROM_HEADER = "Ssp-From";
    private static final String SSP_TO_HEADER = "Ssp-To";
    private static final String SSP_INTERACTION_ID_HEADER = "Ssp-InteractionID";
    private static final String SSP_TRACE_ID_HEADER = "Ssp-TraceID";
    private static final String DOCUMENT_INTERACTION_ID = "urn:nhs:names:services:gpconnect:documents:fhir:rest:read:binary-1";
    private static final String ANY_STRING = "any";

    @Test
    public void When_MakingRequestForSpecificDocument_Expect_OkResponse() {
        WIRE_MOCK_SERVER.stubFor(get(urlPathEqualTo(GET_DOCUMENT_URI))
            .willReturn(aResponse()
                .withStatus(HttpStatus.SC_OK)
                .withBody(EXPECTED_DOCUMENT_BODY)));

        getWebTestClient().get()
            .uri(GET_DOCUMENT_URI)
            .header(SSP_FROM_HEADER, ANY_STRING)
            .header(SSP_TO_HEADER, ANY_STRING)
            .header(SSP_INTERACTION_ID_HEADER, DOCUMENT_INTERACTION_ID)
            .header(SSP_TRACE_ID_HEADER, ANY_STRING)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .json(EXPECTED_DOCUMENT_BODY);
    }

    @Test
    public void When_MakingRequestForSpecificDocument_Expect_ErrorResponse() {
        WIRE_MOCK_SERVER.stubFor(get(urlPathEqualTo(NOT_FOUND_GET_DOCUMENT_URI))
            .willReturn(aResponse()
                .withStatus(HttpStatus.SC_NOT_FOUND)
                .withBody(EXPECTED_NOT_FOUND_BODY)));

        getWebTestClient().get()
            .uri(NOT_FOUND_GET_DOCUMENT_URI)
            .header(SSP_FROM_HEADER, ANY_STRING)
            .header(SSP_TO_HEADER, ANY_STRING)
            .header(SSP_INTERACTION_ID_HEADER, DOCUMENT_INTERACTION_ID)
            .header(SSP_TRACE_ID_HEADER, ANY_STRING)
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectBody()
            .json(EXPECTED_NOT_FOUND_BODY);
    }
}
