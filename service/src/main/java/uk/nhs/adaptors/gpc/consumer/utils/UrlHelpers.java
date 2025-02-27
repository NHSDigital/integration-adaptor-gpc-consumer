package uk.nhs.adaptors.gpc.consumer.utils;

import java.net.URI;
import java.util.List;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ObjectUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UrlHelpers {

    public static String getUrlBase(URI uri, List<String> protocolHeader) {
        if (ObjectUtils.isNotEmpty(protocolHeader)) {
            return protocolHeader.get(0) + "://" + uri.getAuthority();
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
