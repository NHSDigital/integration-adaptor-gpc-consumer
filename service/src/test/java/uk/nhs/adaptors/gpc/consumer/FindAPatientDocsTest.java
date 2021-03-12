package uk.nhs.adaptors.gpc.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import uk.nhs.adaptors.gpc.consumer.filters.SearchForDocumentsGatewayFilterFactory;
import uk.nhs.adaptors.gpc.consumer.utils.FindAPatientDocsUtil;

public class FindAPatientDocsTest {
    private static final String GPC_BASE_URL = "http://localhost:8110";
    private static final String GPC_CONSUMER_BASE_URL = "http://localhost:8080";
    private static final String RESPONSE_BODY_TEMPLATE = "{\"url\": \"%s/example\"}";
    private static final String GPC_RESPONSE_BODY = String.format(RESPONSE_BODY_TEMPLATE, GPC_BASE_URL);

    @Test
    public void When_GPCBaseUrlPresentInBody_Expect_GPCConsumerBaseUrlReplacementInBody() {
        var expectedGpcConsumerResponse = String.format(RESPONSE_BODY_TEMPLATE, GPC_CONSUMER_BASE_URL);
        SearchForDocumentsGatewayFilterFactory.Config config = new SearchForDocumentsGatewayFilterFactory.Config();

        var replacedUrlBody = FindAPatientDocsUtil.replaceUrl(GPC_CONSUMER_BASE_URL, GPC_BASE_URL, GPC_RESPONSE_BODY);
        assertThat(replacedUrlBody).isEqualTo(expectedGpcConsumerResponse);
    }
}
