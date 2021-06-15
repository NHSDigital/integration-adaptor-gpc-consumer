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
    @Value("${gpc-consumer.gpc.overrideGpcProviderUrl}")
    private String overrideGpcProviderUrl;

    public static String replaceUrl(String gpcConsumerUrl, String overrideGpcProviderUrl, String responseBody) {
        LOGGER.info("Replace host: {}, to: {}", overrideGpcProviderUrl, gpcConsumerUrl);
        return responseBody.replace(overrideGpcProviderUrl, gpcConsumerUrl);
    }

    @Override
    public Publisher<String> apply(ServerWebExchange serverWebExchange, String s) {
        // TODO: Steps for NIAD-1283 to enable multiple GPC providers
        // 1) Determine the correct prefix of *this* service - GPC_URL env var
        // 2) Calculate a pattern for the URL we need to replace (based on previous SDS lookup, put onto event context)
        // 3) Perform the replace operation using calculated pattern
        if (s.isBlank()) {
            LOGGER.error("An error with status occurred");
            return Mono.empty();
        } else {
            return Mono.just(UrlsInResponseBodyRewriteFunction.replaceUrl(gpcConsumerUrl, overrideGpcProviderUrl, s));
        }
    }
}
