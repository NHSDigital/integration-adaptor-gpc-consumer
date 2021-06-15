package uk.nhs.adaptors.gpccmocks.common;

import java.util.UUID;

import org.springframework.http.HttpHeaders;

import lombok.NonNull;

public class ControllerHelpers {
    public static boolean isUuid(@NonNull String str) {
        try {
            UUID.fromString(str);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    public static HttpHeaders getResponseHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json+fhir");
        return headers;
    }
}
