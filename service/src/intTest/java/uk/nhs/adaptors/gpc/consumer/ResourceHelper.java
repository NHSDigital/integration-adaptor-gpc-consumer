package uk.nhs.adaptors.gpc.consumer;

import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class ResourceHelper {
    public static String loadClasspathResourceAsString(String path) {
        try (Scanner s = new Scanner(ResourceHelper.class.getResourceAsStream(path), StandardCharsets.UTF_8)) {
            return s.useDelimiter("\\A").next();
        }
    }
}