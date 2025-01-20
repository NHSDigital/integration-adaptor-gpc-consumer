package uk.nhs.adaptors.gpc.consumer.filters;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
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

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SspFilter implements GlobalFilter, Ordered {

    static final int SSP_FILTER_ORDER = SdsFilter.SDS_FILTER_ORDER + 1;

    private final GpcConfiguration gpcConfiguration;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (gpcConfiguration.isSspEnabled()) {
            URI uri = (URI) exchange.getAttributes()
                .get(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR);

            URI resolvedUri = uri.resolve(gpcConfiguration.getSspUrl() + uri);

            exchange.getAttributes()
                .put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, resolvedUri);
        }
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return SSP_FILTER_ORDER;
    }
}