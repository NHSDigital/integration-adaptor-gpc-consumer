package uk.nhs.adaptors.gpc.consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

public class GetStructuredRecordTest extends CloudGatewayTest {
    private static final String STRUCTURED_URI = "/GP0001/STU3/1/gpconnect/fhir/Patient/$gpc.getstructuredrecord";
    private static final String EXAMPLE_STRUCTURED_BODY = "{\"resourceType\":\"Bundle\","
        + "\"meta\":{\"profile\":[\"https://fhir.nhs"
        + ".uk/STU3/StructureDefinition/GPConnect-StructuredRecord-Bundle-1\"]},"
        + "\"type\":\"collection\",\"entry\":[]}";

    @Test
    public void When_MakingRequestForStructuredDocument_Expect_OkResponse() {
        WIRE_MOCK_SERVER.stubFor(post(urlPathEqualTo(STRUCTURED_URI))
            .willReturn(aResponse()
                .withStatus(HttpStatus.SC_OK)
                .withBody(EXAMPLE_STRUCTURED_BODY)));

        getWebTestClient().post()
            .uri(STRUCTURED_URI)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .json(EXAMPLE_STRUCTURED_BODY);
    }

    @Test
    public void When_MakingRequestForNonExistingStructuredDocument_Expect_NotFoundResponse() {
        getWebTestClient().post()
            .uri(STRUCTURED_URI)
            .exchange()
            .expectStatus()
            .isNotFound();
    }
}
