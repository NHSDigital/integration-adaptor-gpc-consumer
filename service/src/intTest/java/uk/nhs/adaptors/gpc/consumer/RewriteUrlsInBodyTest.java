package uk.nhs.adaptors.gpc.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import static uk.nhs.adaptors.gpc.consumer.GetStructuredRecordRouteTest.STRUCTURED_INTERACTION_ID;
import static uk.nhs.adaptors.gpc.consumer.RetrieveADocumentRouteTest.DOCUMENT_INTERACTION_ID;
import static uk.nhs.adaptors.gpc.consumer.SearchForAPatientsDocumentsRouteTest.DOCUMENT_SEARCH_ID;
import static uk.nhs.adaptors.gpc.consumer.SearchForAPatientsDocumentsRouteTest.REQUEST_URI_TEMPLATE;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.web.reactive.server.WebTestClient;
import com.google.common.net.HttpHeaders;

import com.jayway.jsonpath.JsonPath;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gpc.consumer.testcontainers.GpccMocksContainer;
import uk.nhs.adaptors.gpc.consumer.utils.UrlHelpers;

@Slf4j
public class RewriteUrlsInBodyTest extends CloudGatewayRouteBaseTest {

    @ParameterizedTest(name = "{argumentsWithNames} {displayName}")
    @MethodSource(value = "uk.nhs.adaptors.gpc.consumer.Fixtures#orgCodes")
    public void When_SearchForAPatientsDocuments_Expect_AttachmentUrlIsAccessibleBinaryResource(String odsCode) {
        var patientId = Fixtures.Patient.HAS_DOCUMENTS.getAccessDocumentLogicalId();
        var requestUri = String.format(REQUEST_URI_TEMPLATE, odsCode, patientId);
        var bodyBytes = getWebTestClientForStandardGet(requestUri, DOCUMENT_SEARCH_ID)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .returnResult()
            .getResponseBody();
        var body = new String(bodyBytes, StandardCharsets.UTF_8);
        var url = getUrlFromFirstDocumentReference(body);

        assertThat(url)
            .startsWith(getGpccAdaptorBaseUri())
            .doesNotStartWith(GpccMocksContainer.getInstance().getMockBaseUrl())
            .contains(odsCode);
        assertThatUrlIsAccessibleBinaryResource(url);
    }

    @ParameterizedTest(name = "{argumentsWithNames} {displayName}")
    @MethodSource(value = "uk.nhs.adaptors.gpc.consumer.Fixtures#orgCodes")
    public void When_GetStructuredRecord_Expect_AttachmentUrlIsAccessibleBinaryResource(String odsCode) {
        var nhsNumber = Fixtures.Patient.HAS_DOCUMENTS.getNhsNumber();
        var requestUri = String.format(GetStructuredRecordRouteTest.REQUEST_URI_TEMPLATE, odsCode);
        var requestBody = String.format(GetStructuredRecordRouteTest.REQUEST_BODY_TEMPLATE, nhsNumber);
        var bodyBytes = getWebTestClientForStandardPost(requestUri, STRUCTURED_INTERACTION_ID)
            .bodyValue(requestBody)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .returnResult()
            .getResponseBody();
        var body = new String(bodyBytes, StandardCharsets.UTF_8);
        var url = getUrlFromFirstDocumentReference(body);

        assertThat(url)
            .startsWith(getGpccAdaptorBaseUri())
            .doesNotStartWith(GpccMocksContainer.getInstance().getMockBaseUrl())
            .contains(odsCode);
        assertThatUrlIsAccessibleBinaryResource(url);
    }

    @ParameterizedTest(name = "{argumentsWithNames} {displayName}")
    @MethodSource(value = "uk.nhs.adaptors.gpc.consumer.Fixtures#orgCodes")
    public void When_MigrateStructuredRecord_Expect_UrlsAreRewrittenWith(String odsCode) {
        var nhsNumber = Fixtures.Patient.HAS_DOCUMENTS.getNhsNumber();
        var requestUri = String.format(MigrateStructuredPatientRouteTest.REQUEST_URI_TEMPLATE, odsCode);
        var requestBody = String.format(MigrateStructuredPatientRouteTest.REQUEST_BODY_TEMPLATE, nhsNumber);

        var bodyBytes = getWebTestClientForStandardPost(requestUri, MigrateStructuredPatientRouteTest.MIGRATE_STRUCTURED_INTERACTION_ID)
            .bodyValue(requestBody)
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .returnResult();

        var body = new String(bodyBytes.getResponseBody(), StandardCharsets.UTF_8);
        var url = getUrlFromFirstDocumentReference(body);

        assertThat(url)
            .startsWith(getGpccAdaptorBaseUri())
            .doesNotStartWith(GpccMocksContainer.getInstance().getMockBaseUrl())
            .contains(odsCode);
        assertThatUrlIsAccessibleBinaryResource(url);
    }

    private void assertThatUrlIsAccessibleBinaryResource(String url) {
        LOGGER.info("Asserting that URL is an accessible binary resource: {}", url);
        var binaryId = url.substring(url.lastIndexOf("/") + 1);
        createRequestForUrl(url)
            .header(SSP_FROM_HEADER, ANY_STRING)
            .header(SSP_TO_HEADER, ANY_STRING)
            .header(SSP_INTERACTION_ID_HEADER, DOCUMENT_INTERACTION_ID)
            .header(SSP_TRACE_ID_HEADER, SSP_TRACEID_VALUE)
            .header(HttpHeaders.AUTHORIZATION, "anytoken")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody()
            .jsonPath("$.id").isEqualTo(binaryId);
    }

    private String getUrlFromFirstDocumentReference(String bundleJson) {
        return ((List<?>) JsonPath.read(bundleJson, "$.entry[*].resource.content[*].attachment.url"))
            .stream()
            .findFirst()
            .map(String.class::cast)
            .orElse("");
    }

    @SneakyThrows
    private WebTestClient.RequestHeadersSpec<?> createRequestForUrl(String urlString) {
        URI url = new URI(urlString);
        var urlBase = UrlHelpers.getUrlBase(url);
        return WebTestClient.bindToServer()
            .baseUrl(urlBase)
            .build()
            .get()
            .uri(url);
    }

}
