package uk.nhs.adaptors.gpc.consumer.filters;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import uk.nhs.adaptors.gpc.consumer.gpc.GpcConfiguration;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SspFilter implements GlobalFilter, Ordered {
    static final int SSP_FILTER_ORDER = SdsFilter.SDS_FILTER_ORDER + 1;

    private final GpcConfiguration gpcConfiguration;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (isSspEnabled()) {
            var sspPrefix = gpcConfiguration.getSspDomain();
            LOGGER.info("SSP is enabled. Prepending the destination URL the the SSP URL: {}", sspPrefix);
            exchange.addUrlTransformer(this::transformUrl);
        } else {
            LOGGER.info("SSP is disabled");
        }
        return chain.filter(exchange);
    }

    private String transformUrl(String url) {
        var transformed = gpcConfiguration.getSspDomain() + url;
        LOGGER.info("Replacing original destination URL {} with the SSP url {}", url, transformed);
        return transformed;
    }

    private boolean isSspEnabled() {
        return StringUtils.isNotBlank(gpcConfiguration.getSspDomain());
    }

    @Override
    public int getOrder() {
        return SSP_FILTER_ORDER;
    }
}
