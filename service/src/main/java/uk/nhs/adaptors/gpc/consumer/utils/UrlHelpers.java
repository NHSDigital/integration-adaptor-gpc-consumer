package uk.nhs.adaptors.gpc.consumer.utils;

import java.net.URI;

import lombok.SneakyThrows;

public class UrlHelpers {

    public static String getUrlBase(URI uri) {
        return uri.getScheme() + "://" + uri.getAuthority();
    }

    @SneakyThrows
    public static String getUrlBase(String url) {
        return getUrlBase(new URI(url));
    }

}
