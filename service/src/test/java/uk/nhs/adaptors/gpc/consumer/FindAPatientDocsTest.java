package uk.nhs.adaptors.gpc.consumer;

import static org.apache.commons.lang3.StringUtils.EMPTY;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.util.IOUtils;
import uk.nhs.adaptors.gpc.consumer.utils.FindAPatientDocsUtil;

public class FindAPatientDocsTest {
    private static final String GPC_BASE_URL = "http://localhost:8110";
    private static final String GPC_CONSUMER_BASE_URL = "http://localhost:8080";

    private final InputStream inputStreamExpectedPatientsDocumentWithMultipleUrl = this.getClass().getClassLoader().getResourceAsStream("ExpectedSearchForPatientsDocumentMultipleUrl.json");
    private final InputStream inputStreamExpectedPatientsDocumentWithSingleUrl = this.getClass().getClassLoader().getResourceAsStream("ExpectedSearchForPatientsDocumentSingleUrl.json");
    private final InputStream inputStreampatientsDocumentWithMultipleUrl = this.getClass().getClassLoader().getResourceAsStream("SearchForPatientsDocumentMultipleUrl.json");
    private final InputStream inputStreampatientsDocumentWithSingleUrl = this.getClass().getClassLoader().getResourceAsStream("SearchForPatientsDocumentSingleUrl.json");
    private final InputStream inputStreampatientsDocumentWithNoUrl = this.getClass().getClassLoader().getResourceAsStream("SearchForPatientsDocumentWithNoUrl.json");

    @Test
    public void When_GPCBaseUrlPresentInBody_Expect_GPCConsumerBaseUrlReplacementInBody() {
        String response = getResourceAsString(inputStreampatientsDocumentWithSingleUrl);
        String expected = getResourceAsString(inputStreamExpectedPatientsDocumentWithSingleUrl);
        var replacedUrlBody = FindAPatientDocsUtil.replaceUrl(GPC_CONSUMER_BASE_URL, GPC_BASE_URL, response);
        assertThat(replacedUrlBody).isEqualTo(expected);
    }

    @Test
    public void When_GPCMultipleBaseUrlPresentInBody_Expect_GPCConsumerBaseUrlMultipleReplacementInBody() {
        String response = getResourceAsString(inputStreampatientsDocumentWithMultipleUrl);
        String expected = getResourceAsString(inputStreamExpectedPatientsDocumentWithMultipleUrl);

        var replacedUrlBody = FindAPatientDocsUtil.replaceUrl(GPC_CONSUMER_BASE_URL, GPC_BASE_URL, response);
        assertThat(replacedUrlBody).isEqualTo(expected);
    }

    @Test
    public void When_GPCBaseUrlNotPresentInBody_Expect_NoChangeInResponseBody() {
        String response = getResourceAsString(inputStreampatientsDocumentWithNoUrl);

        var replacedUrlBody = FindAPatientDocsUtil.replaceUrl(GPC_CONSUMER_BASE_URL, GPC_BASE_URL, response);
        assertThat(replacedUrlBody).isEqualTo(replacedUrlBody);
    }

    @Test
    public void When_EmptyString_Expect_EmptyString() {
        var response = String.format(EMPTY, GPC_CONSUMER_BASE_URL);

        var replacedUrlBody = FindAPatientDocsUtil.replaceUrl(GPC_CONSUMER_BASE_URL, GPC_BASE_URL, response);
        assertThat(replacedUrlBody).isEqualTo(response);
    }

    private String getResourceAsString(InputStream resourceName) {
        return IOUtils.toString(resourceName, StandardCharsets.UTF_8);
    }
}
