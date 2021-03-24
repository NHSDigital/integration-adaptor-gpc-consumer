package uk.nhs.adaptors.gpc.consumer.utils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

public class QueryParamsEncoder {
    private QueryParamsEncoder() {
    }

    public static void encodeQueryParams(ServerWebExchange exchange) {
        Map<String, Object> attributes = exchange.getAttributes();

        if (attributes.containsKey(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR)) {
            MultiValueMap<String, String> encodedParams = new LinkedMultiValueMap<>();
            exchange.getRequest().getQueryParams().forEach(
                (key, values) -> values.forEach(
                    value -> encodedParams.add(UriUtils.encode(key, StandardCharsets.UTF_8),
                        UriUtils.encode(value, StandardCharsets.UTF_8))));

            URI uri = (URI) attributes.get(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
            attributes.put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR,
                prepareUriWithEncodedParams(uri, encodedParams));
        }
    }

    private static URI prepareUriWithEncodedParams(URI uri, MultiValueMap<String, String> encodedParams) {
        return UriComponentsBuilder.fromUri(uri)
            .replaceQueryParams(encodedParams)
            .build(true)
            .toUri();
    }
}
