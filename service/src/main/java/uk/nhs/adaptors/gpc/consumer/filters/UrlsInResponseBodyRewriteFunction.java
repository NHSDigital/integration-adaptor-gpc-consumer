package uk.nhs.adaptors.gpc.consumer.filters;

import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class UrlsInResponseBodyRewriteFunction implements RewriteFunction<String, String> {
    @Value("${gpc-consumer.gpc.gpcConsumerUrl}")
    private String gpcConsumerUrl;
    @Value("${gpc-consumer.gpc.gpcUrl}")
    private String gpcUrl;

    public static String replaceUrl(String gpcConsumerUrl, String gpcUrl, String responseBody) {
        LOGGER.info(String.format("Replace host: %s, to: %s", gpcUrl, gpcConsumerUrl));
        return responseBody.replace(gpcUrl, gpcConsumerUrl);
    }

    @Override
    public Publisher<String> apply(ServerWebExchange serverWebExchange, String s) {
        // 1) Determine the correct prefix of *this* service
        // 2) Calculate a pattern for the URL we need to replace (based on previous SDS lookup?)
        // 3) Perform the replace operation using calculated pattern
        if (s.isBlank()) {
            LOGGER.error("An error with status occurred");
            return Mono.empty();
        } else {
            return Mono.just(UrlsInResponseBodyRewriteFunction.replaceUrl(gpcConsumerUrl, gpcUrl, s));
        }
    }
}
