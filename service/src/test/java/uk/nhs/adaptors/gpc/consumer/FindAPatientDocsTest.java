package uk.nhs.adaptors.gpc.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gpc.consumer.filters.FindAPatientDocsGatewayFilterFactory;
import uk.nhs.adaptors.gpc.consumer.utils.FindAPatientDocsUtil;

public class FindAPatientDocsTest {
    private static final String GPC_BASE_URL = "http://localhost:8110";
    private static final String GPC_CONSUMER_BASE_URL = "http://localhost:8080";
    private static final String RESPONSE_BODY_TEMPLATE = "{\"url\": \"%s/example\"}";
    private static final String GPC_RESPONSE_BODY = String.format(RESPONSE_BODY_TEMPLATE, GPC_BASE_URL);

    @Test
    public void When_GPCBaseUrlPresentInBody_Expect_GPCConsumerBaseUrlReplacementInBody() {
        var expectedGpcConsumerResponse = String.format(RESPONSE_BODY_TEMPLATE, GPC_CONSUMER_BASE_URL);
        FindAPatientDocsGatewayFilterFactory.Config config = new FindAPatientDocsGatewayFilterFactory.Config();
        config.setGpcUrl(GPC_BASE_URL);
        config.setGpcConsumerUrl(GPC_CONSUMER_BASE_URL);

        var replacedUrlBody = FindAPatientDocsUtil.replaceUrl(config, GPC_RESPONSE_BODY);
        assertThat(replacedUrlBody).isEqualTo(expectedGpcConsumerResponse);
    }

    @Test
    public void When_ZipPramsString_Expect_ByteArrayOutputStream() throws Exception {
        var os = FindAPatientDocsUtil.zipStringToOutputStream(GPC_RESPONSE_BODY);
        assertThat(os.getClass()).isEqualTo(ByteArrayOutputStream.class);
    }

    @Test
    public void When_ZipPramsNull_Expect_ExceptionThrow() {
        var exception = assertThrows(Exception.class, () -> FindAPatientDocsUtil.zipStringToOutputStream(null));
        assertThat(exception.getMessage()).isEqualTo("Error occuring compressing response");
    }

    @Test
    public void When_UnzipPramsInputStream_Expect_String() throws Exception {
        InputStream targetStream = new ByteArrayInputStream(GPC_RESPONSE_BODY.getBytes());
        var is = createGzipInputStreamFromString(targetStream);

        var os = FindAPatientDocsUtil.unzipInputStreamToString(is);
        assertThat(os).isEqualTo(GPC_RESPONSE_BODY);
    }

    @Test
    public void When_UnzipPramsNull_Expect_String() {
        var exception = assertThrows(Exception.class, () -> FindAPatientDocsUtil.unzipInputStreamToString(null));
        assertThat(exception.getMessage()).isEqualTo("Error occuring decompressing response");
    }

    @SuppressWarnings("InnerAssignment")
    @SneakyThrows
    public static InputStream createGzipInputStreamFromString(InputStream inputStream) {
        ByteArrayOutputStream bytesOutput = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutput = new GZIPOutputStream(bytesOutput);
        final int size = 10240;
        byte[] buffer = new byte[size];
        for (int length = 0; (length = inputStream.read(buffer)) != -1;) {
            gzipOutput.write(buffer, 0, length);
        }
        inputStream.close();
        gzipOutput.close();
        var zipInputStream = new ByteArrayInputStream(bytesOutput.toByteArray());
        return zipInputStream;
    }
}
