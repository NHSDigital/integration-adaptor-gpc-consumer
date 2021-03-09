package uk.nhs.adaptors.gpc.consumer.common;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;

public class ResourceReader {
    public static String asString(Resource resource) {
        try {
            return IOUtils.toString(resource.getInputStream(), UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}