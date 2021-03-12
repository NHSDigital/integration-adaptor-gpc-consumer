package uk.nhs.adaptors.gpc.consumer;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;

public class FindPatientRouteTest extends CloudGatewayRouteBaseTest {

    private static final String FIND_PATIENT_URI = FHIR_PATIENT_URI + "?identifier=https://fhir.nhs.uk/Id/nhs-number|9690937286";
    private static final String FIND_PATIENT_URI_ENCODED = FHIR_PATIENT_URI + "?identifier=https://fhir.nhs.uk/Id/nhs-number%7C9690937286";
    private static final String EXAMPLE_FIND_PATIENT_BODY = "{\"resourceType\":\"Bundle\",\"id\":\"2fd9c6e5-0197-4a78-923d-f8ed3c937880"
        + "\",\"meta\":{\"lastUpdated\":\"2021-03-04T15:40:22.932+00:00\"},\"type\":\"searchset\","
        + "\"entry\":[{\"resource\":{\"resourceType\":\"Patient\",\"id\":\"2\"}}]}";

    @Test
    public void When_MakingRequestForFindPatient_Expect_OkResponse() {
        WIRE_MOCK_SERVER.stubFor(get(urlEqualTo(FIND_PATIENT_URI_ENCODED))
            .willReturn(aResponse()
                .withStatus(HttpStatus.SC_OK)
                .withBody(EXAMPLE_FIND_PATIENT_BODY)));

        getWebTestClient().get()
            .uri(FIND_PATIENT_URI)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .json(EXAMPLE_FIND_PATIENT_BODY);
    }

    @Test
    public void When_MakingRequestForFindPatientWithoutIdentifier_Expect_NotFoundResponse() {
        getWebTestClient().get()
            .uri(FHIR_PATIENT_URI)
            .exchange()
            .expectStatus()
            .isNotFound();
    }
}
