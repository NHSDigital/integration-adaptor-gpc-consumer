package uk.nhs.adaptors.gpc.consumer.filters;

import static uk.nhs.adaptors.gpc.consumer.utils.HeaderConstants.GATEWAY_REQUEST_URL_ATTR_BACKUP;
import static uk.nhs.adaptors.gpc.consumer.utils.UrlHelpers.getUrlBase;

import java.net.URI;

import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import uk.nhs.adaptors.gpc.consumer.utils.LoggingUtil;

@Slf4j
@Component
public class UrlsInResponseBodyRewriteFunction implements RewriteFunction<String, String> {
    @Value("${gpc-consumer.gpc.overrideGpcProviderUrl}")
    private String overrideGpcProviderUrl;

    public static String replaceUrl(String gpcConsumerUrl, String overrideGpcProviderUrl, String responseBody) {
        return responseBody.replace(overrideGpcProviderUrl, gpcConsumerUrl);
    }

    @Override
    public Publisher<String> apply(ServerWebExchange exchange, String responseBody) {
        return Mono.just(responseBody)
            .map(originalResponseBody -> {
                var gpcConsumerUrlPrefix = getUrlBase(exchange.getRequest().getURI());
                LoggingUtil.debug(LOGGER, exchange, "The URL prefix for *this* GPC Consumer service is {}", gpcConsumerUrlPrefix);

                URI proxyTargetUri = null;
                if (exchange.getAttributes().containsKey(GATEWAY_REQUEST_URL_ATTR_BACKUP)) {
                    proxyTargetUri = (URI) exchange.getAttributes().get(GATEWAY_REQUEST_URL_ATTR_BACKUP);
                } else {
                    proxyTargetUri = (URI) exchange.getAttributes().get(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);
                }

                var gpcProducerUrlPrefix = getUrlBase(proxyTargetUri);
                LoggingUtil.info(LOGGER, exchange, "The URL prefix of the GPC Producer endpoint is {}", gpcProducerUrlPrefix);

                LOGGER.debug("Replacing all occurrences of '{}' in the response body with '{}'",
                    gpcProducerUrlPrefix, gpcConsumerUrlPrefix);
                return originalResponseBody.replace(gpcProducerUrlPrefix, gpcConsumerUrlPrefix);
            });
    }
}
