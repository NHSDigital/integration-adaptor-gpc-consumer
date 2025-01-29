package uk.nhs.adaptors.gpc.consumer.filters;

import static uk.nhs.adaptors.gpc.consumer.utils.UrlHelpers.getUrlBase;

import java.net.URI;

import com.google.common.net.HttpHeaders;
import lombok.RequiredArgsConstructor;
import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import uk.nhs.adaptors.gpc.consumer.gpc.GpcConfiguration;
import uk.nhs.adaptors.gpc.consumer.utils.LoggingUtil;

@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UrlsInResponseBodyRewriteFunction implements RewriteFunction<String, String> {

    private final GpcConfiguration config;

    @Override
    public Publisher<String> apply(ServerWebExchange exchange, String responseBody) {
        return Mono.just(responseBody)
            .map(originalResponseBody -> {

                var gpcConsumerUrlPrefix = getUrlBase(exchange.getRequest().getURI(),
                    exchange.getRequest().getHeaders().get(HttpHeaders.X_FORWARDED_PROTO));
                LoggingUtil.debug(LOGGER, exchange, "The URL prefix for *this* GPC Consumer service is {}", gpcConsumerUrlPrefix);

                URI proxyTargetUri = (URI) exchange.getAttributes().get(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);

                if (config.isSspEnabled()) {
                    var uriAsStringWithoutSspPrefix = proxyTargetUri.toString().substring(config.getSspUrl().length());
                    proxyTargetUri = proxyTargetUri.resolve(uriAsStringWithoutSspPrefix);
                }

                var gpcProducerUrlPrefix = getUrlBase(proxyTargetUri);
                LoggingUtil.info(LOGGER, exchange, "The URL prefix of the GPC Producer endpoint is {}", gpcProducerUrlPrefix);

                LOGGER.info("Replacing all occurrences of '{}' in the response body with '{}'",
                    gpcProducerUrlPrefix, gpcConsumerUrlPrefix);
                return originalResponseBody.replaceAll("(?i)" + gpcProducerUrlPrefix, gpcConsumerUrlPrefix);
            });
    }
}
