package uk.nhs.adaptors.gpc.consumer.filters;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import uk.nhs.adaptors.gpc.consumer.gpc.GpcConfiguration;
import uk.nhs.adaptors.gpc.consumer.utils.LoggingUtil;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class DirectGpcFilter implements GlobalFilter, Ordered {
    private static final String GPC_URL_ENVIRONMENT_VARIABLE = "GPC_CONSUMER_OVERRIDE_GPC_PROVIDER_URL";

    static final int DIRECT_FILTER_ORDER = SdsFilter.SDS_FILTER_ORDER - 1;

    private final GpcConfiguration gpcConfiguration;

    @Value("${gpc-consumer.sds.enableSDS}")
    private String enableSds;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        var gpcProviderUrlOverride = System.getenv(GPC_URL_ENVIRONMENT_VARIABLE);
        if (!Boolean.parseBoolean(enableSds)) {
            LoggingUtil.info(LOGGER, exchange, "SDS is not enabled. Using the value of {} for the GPC Provider endpoint: {}",
                GPC_URL_ENVIRONMENT_VARIABLE, gpcProviderUrlOverride);
            URI requestUri = exchange.getRequest().getURI();
            URI resolvedUri = requestUri.resolve(
                requestUri
                    .toString()
                    .replace(gpcConfiguration.getGpcConsumerUrl(), getDirectGpcUrl())
            );

            exchange.getAttributes()
                .put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, resolvedUri);
        }

        return chain.filter(exchange);
    }

    private String getDirectGpcUrl() {
        return gpcConfiguration.getGpcUrl();
    }

    @Override
    public int getOrder() {
        return DIRECT_FILTER_ORDER;
    }
}
