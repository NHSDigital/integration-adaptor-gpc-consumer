package uk.nhs.adaptors.gpc.consumer.utils;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gpc.consumer.FindAPatientDocsException;
import uk.nhs.adaptors.gpc.consumer.filters.FindAPatientDocsGatewayFilterFactory;

@UtilityClass
@Slf4j
public class FindAPatientDocsUtil {
    public static String replaceUrl(FindAPatientDocsGatewayFilterFactory.Config config, String body) {
        LOGGER.info(String.format("Replace host: %s, to: %s", config.getGpcUrl(), config.getGpcConsumerUrl()));
        return body.replace(config.getGpcUrl(), config.getGpcConsumerUrl());
    }

    public ByteArrayOutputStream zipStringToOutputStream(String responseWithProxyUrlReplacement) throws FindAPatientDocsException {
        try {
            ByteArrayOutputStream obj = new ByteArrayOutputStream();
            GZIPOutputStream gzip = new GZIPOutputStream(obj);
            gzip.write(responseWithProxyUrlReplacement.getBytes(UTF_8));
            gzip.close();

            LOGGER.info("Response body successfully compress");

            return obj;
        } catch (Exception e) {
            LOGGER.error(String.format("Error occurring compressing response: %s", e.getMessage()));
            throw new FindAPatientDocsException("Error occurring compressing response");
        }
    }

    public String unzipInputStreamToString(InputStream inputStream) throws FindAPatientDocsException {
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
            LOGGER.error(String.format("Error occurring decompressing response: %s", e.getMessage()));
            throw new FindAPatientDocsException("Error occurring decompressing response");
        }
    }

}
