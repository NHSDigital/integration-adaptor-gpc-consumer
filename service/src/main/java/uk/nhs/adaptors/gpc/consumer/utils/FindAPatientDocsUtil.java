package uk.nhs.adaptors.gpc.consumer.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class FindAPatientDocsUtil {
    public static String replaceUrl(String gpcConsumerUrl, String gpcUrl, String responseBody) {
        LOGGER.info(String.format("Replace host: %s, to: %s", gpcUrl, gpcConsumerUrl));
        return responseBody.replace(gpcUrl, gpcConsumerUrl);
    }
}
