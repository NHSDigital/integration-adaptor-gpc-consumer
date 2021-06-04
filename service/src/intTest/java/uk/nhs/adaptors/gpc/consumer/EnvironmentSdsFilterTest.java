package uk.nhs.adaptors.gpc.consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

@ExtendWith(SystemStubsExtension.class)
public class EnvironmentSdsFilterTest extends CloudGatewayRouteBaseTest {
    @SystemStub
    @SuppressWarnings("unused")
    private final EnvironmentVariables environmentVariables
        = new EnvironmentVariables().set(GPC_URL_ENVIRONMENT_VARIABLE_NAME, WIRE_MOCK_SERVER.baseUrl());

    @Test
    public void When_MakingRequestViaSdsFilter_Given_OverrideGpcProviderUrlEnvVariable_Expect_OkResponse() {
        WIRE_MOCK_SERVER.stubFor(get(urlPathEqualTo(GET_DOCUMENT_URI))
            .willReturn(aResponse()
                .withStatus(HttpStatus.SC_OK)
                .withBody(EXPECTED_DOCUMENT_BODY)));

        getWebTestClient().get()
            .uri(GET_DOCUMENT_URI)
            .header(SSP_FROM_HEADER, ANY_STRING)
            .header(SSP_TO_HEADER, ANY_STRING)
            .header(SSP_INTERACTION_ID_HEADER, DOCUMENT_INTERACTION_ID)
            .header(SSP_TRACE_ID_HEADER, RANDOM_UUID)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .json(EXPECTED_DOCUMENT_BODY);
    }
}
