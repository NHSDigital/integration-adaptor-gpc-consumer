package uk.nhs.adaptors.gpccmocks.common;

import java.net.URI;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;

import lombok.NonNull;
import lombok.SneakyThrows;

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

    public static HttpHeaders getResponseHeadersWithForwardedProto() {
        HttpHeaders httpHeaders = getResponseHeaders();
        httpHeaders.add("X-Forwarded-Proto", "https");
        return httpHeaders;
    }

    @SneakyThrows
    public static String getHostAndPortFromRequest(HttpServletRequest httpServletRequest) {
        URI uri = new URI(httpServletRequest.getRequestURL().toString());
        return uri.getAuthority();
    }

}
