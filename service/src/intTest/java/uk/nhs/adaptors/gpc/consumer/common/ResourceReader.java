package uk.nhs.adaptors.gpc.consumer.common;

import static java.nio.charset.StandardCharsets.UTF_8;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;

import lombok.SneakyThrows;

public class ResourceReader {
    @SneakyThrows
    public static String asString(Resource resource) {
        return IOUtils.toString(resource.getInputStream(), UTF_8);
    }
}