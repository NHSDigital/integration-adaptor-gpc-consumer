package uk.nhs.adaptors.gpc.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;

public class FindAPatientDocsTest {
    private static final String GPC_BASE_URL = "http://localhost:8110";
    private static final String GPC_CONSUMER_BASE_URL = "http://localhost:8080";
    private static final String RESPONSE_BODY_TEMPLATE = "{\"url\": \"%s/example\"}";
    private static final String GPC_RESPONSE_BODY = String.format(RESPONSE_BODY_TEMPLATE, GPC_BASE_URL);

    @Mock
    private GZIPInputStream gzipInputStream;

    @Test
    public void When_GPCBaseUrlPresentInBody_Expect_GPCConsumerBaseUrlReplacementInBody() {
        var expectedGpcConsumerResponse = String.format(RESPONSE_BODY_TEMPLATE, GPC_CONSUMER_BASE_URL);
        FindAPatientDocsGatewayFilterFactory.Config config = new FindAPatientDocsGatewayFilterFactory.Config();
        config.setTargetUrl(GPC_BASE_URL);
        config.setGpcConsumerurl(GPC_CONSUMER_BASE_URL);

        var replacedUrlBody = FindAPatientDocsUtil.replaceUrl(config, GPC_RESPONSE_BODY);
        assertThat(replacedUrlBody).isEqualTo(expectedGpcConsumerResponse);
    }

    @Test
    public void When_ZipPramsString_Expect_ByteArrayOutputStream() throws Exception {
        var os = FindAPatientDocsUtil.zipStringToOutputStream(GPC_RESPONSE_BODY);
        assertThat(os.getClass()).isEqualTo(ByteArrayOutputStream.class);
    }

    @Test
    public void When_ZipPramsNull_Expect_ExceptionThrow() throws Exception {
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
    public void When_UnzipPramsNull_Expect_String() throws Exception {
        var exception = assertThrows(Exception.class, () -> FindAPatientDocsUtil.unzipInputStreamToString(null));
        assertThat(exception.getMessage()).isEqualTo("Error occuring decompressing response");
    }

    public static InputStream createGzipInputStreamFromString(InputStream inputStream) throws IOException
    {
        InputStream zipInputStream = null;
        try {
            ByteArrayOutputStream bytesOutput = new ByteArrayOutputStream();
            GZIPOutputStream gzipOutput = new GZIPOutputStream(bytesOutput);

            try {
                byte[] buffer = new byte[10240];
                for (int length = 0; (length = inputStream.read(buffer)) != -1;) {
                    gzipOutput.write(buffer, 0, length);
                }
            } finally {
                try { inputStream.close(); } catch (IOException ignore) {}
                try { gzipOutput.close(); } catch (IOException ignore) {}
            }

            zipInputStream = new ByteArrayInputStream(bytesOutput.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return zipInputStream;
    }
}
