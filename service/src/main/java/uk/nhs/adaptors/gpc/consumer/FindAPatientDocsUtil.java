package uk.nhs.adaptors.gpc.consumer;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class FindAPatientDocsUtil {

    public static String replaceUrl(FindAPatientDocsGatewayFilterFactory.Config config, String body) {
        LOGGER.info(String.format("Replace host: %s, to: %s", config.getTargetUrl(), config.getGpcConsumerurl()));
        return body.replace(config.getTargetUrl(), config.getGpcConsumerurl());
    }

    public ByteArrayOutputStream zipStringToOutputStream(String responseWithProxyUrlReplacement) throws Exception {
        try {
            ByteArrayOutputStream obj = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(obj);
            gzip.write(responseWithProxyUrlReplacement.getBytes(UTF_8));
            gzip.close();

            LOGGER.info("Response body successfully compress");

            return obj;
        } catch (Exception e) {
            LOGGER.error("Error occuring compressing response: %s", e);
            throw new Exception("Error occuring compressing response", e);
        }
    }

    public String unzipInputStreamToString(InputStream inputStream) throws Exception {
        try {
            StringBuilder outStr = new StringBuilder();
            GZIPInputStream gis = new GZIPInputStream(inputStream);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(gis, UTF_8));

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                outStr.append(line);
            }
            bufferedReader.close();

            LOGGER.info("Response body successfully uncompressed");

            return outStr.toString();
        } catch (Exception e) {
            LOGGER.error("Error occuring decompressing response: %s", e);
            throw new Exception("Error occuring decompressing response", e);
        }
    }

}
