package uk.nhs.adaptors.gpc.consumer.utils;

import java.net.URI;
import java.util.List;

import lombok.SneakyThrows;
import org.apache.commons.lang3.ObjectUtils;

public class UrlHelpers {

    public static String getUrlBase(URI uri, List<String> protocolHeader) {
        if (ObjectUtils.isNotEmpty(protocolHeader)) {
            String protocol = String.valueOf(protocolHeader);
            return protocol + "://" + uri.getAuthority();
        }
        return getUrlBase(uri);
    }

    public static String getUrlBase(URI uri) {
        return uri.getScheme() + "://" + uri.getAuthority();
    }

    @SneakyThrows
    public static String getUrlBase(String url) {
        return getUrlBase(new URI(url));
    }


}
