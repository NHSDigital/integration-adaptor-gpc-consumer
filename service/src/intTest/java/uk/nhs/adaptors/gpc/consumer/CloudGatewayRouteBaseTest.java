package uk.nhs.adaptors.gpc.consumer;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

import lombok.Getter;
import uk.nhs.adaptors.gpc.consumer.testcontainers.GpccMockExtension;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(GpccMockExtension.class)
public class CloudGatewayRouteBaseTest {
    protected static final String SSP_FROM_HEADER = "Ssp-From";
    protected static final String SSP_TO_HEADER = "Ssp-To";
    protected static final String SSP_INTERACTION_ID_HEADER = "Ssp-InteractionID";
    protected static final String SSP_TRACE_ID_HEADER = "Ssp-TraceID";
    protected static final String ANY_STRING = "any";
    protected static final String SSP_TRACEID_VALUE = "921fef26-764b-4a36-80b3-a835abde3304";

    private static final int MAX_TIMEOUT = 10;
    private static final String LOCALHOST_URI = "http://localhost:";
    private static final int MAX_IN_MEMORY_BYTES = 100 * 1024 * 1024;
    @LocalServerPort
    private int port;
    @Getter
    private WebTestClient webTestClient;
    @Getter
    private String gpccAdaptorBaseUri;

    @BeforeEach
    public void setUp() {
        gpccAdaptorBaseUri = LOCALHOST_URI + port;
        webTestClient = WebTestClient.bindToServer()
            .responseTimeout(Duration.ofSeconds(MAX_TIMEOUT))
            .baseUrl(gpccAdaptorBaseUri)
            .exchangeStrategies(ExchangeStrategies.builder().codecs(configurer -> configurer
                .defaultCodecs()
                .maxInMemorySize(MAX_IN_MEMORY_BYTES))
                .build())
            .build();
    }

    protected WebTestClient.RequestBodySpec getWebTestClientForStandardPost(String requestUri, String interactionId) {
        return webTestClient.post()
            .uri(requestUri)
            .header(SSP_FROM_HEADER, ANY_STRING)
            .header(SSP_TO_HEADER, ANY_STRING)
            .header(SSP_INTERACTION_ID_HEADER, interactionId)
            .header(SSP_TRACE_ID_HEADER, SSP_TRACEID_VALUE)
            .header(HttpHeaders.AUTHORIZATION, "anytoken");
    }

    protected void When_GetRequestProducesSdsError_Expect_OperationOutcomeErrorResponse(String requestUri, String interactionId) {
        // Using Ssp-TraceID not a UUID to force an error from the SDS mock
        webTestClient.get().uri(requestUri)
            .header(SSP_FROM_HEADER, ANY_STRING)
            .header(SSP_TO_HEADER, ANY_STRING)
            .header(SSP_INTERACTION_ID_HEADER, interactionId)
            .header(SSP_TRACE_ID_HEADER, "NotUUID")
            .header(HttpHeaders.AUTHORIZATION, "anytoken")
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        // TODO: NIAD-1165 GPCC should use the SDS API OperationOutcome here instead of a Spring default error response body
    }

    protected WebTestClient.RequestHeadersSpec<?> getWebTestClientForStandardGet(String requestUri, String interactionId) {
        return webTestClient.get()
            .uri(requestUri)
            .header(SSP_FROM_HEADER, ANY_STRING)
            .header(SSP_TO_HEADER, ANY_STRING)
            .header(SSP_INTERACTION_ID_HEADER, interactionId)
            .header(SSP_TRACE_ID_HEADER, SSP_TRACEID_VALUE)
            .header(HttpHeaders.AUTHORIZATION, "anytoken");
    }

}
