package uk.nhs.adaptors.gpc.consumer;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class LoggingGlobalFilter implements Ordered, GlobalFilter {
    private static final List<String> LOGGABLE_HEADER_KEYS = List.of("Ssp-From", "Ssp-To", "Ssp-TraceID");
    private static final String PROXY_LOG_TEMPLATE = "Global filter log: %s Request Url: %s, Destination Request Url: %s";
    private static final String LOG_TEMPLATE = "Global filter log: %s Request Url: %s";
    private static final String HEADERS_PREFIX = "Headers: ";
    private static final String EQUAL_SIGN = " = ";
    private static final int PRIORITY = -2;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
        if (route != null) {
            LOGGER.info(String.format(PROXY_LOG_TEMPLATE,
                prepareHeaderLog(exchange.getRequest().getHeaders()),
                exchange.getRequest().getURI(),
                route.getUri() + exchange.getRequest().getPath().toString()));
        } else {
            LOGGER.info(String.format(LOG_TEMPLATE,
                prepareHeaderLog(exchange.getRequest().getHeaders()),
                exchange.getRequest().getURI()));
        }

        return chain.filter(exchange.mutate().response(prepareErrorHandlingResponseDecorator(exchange)).build());
    }

    @Override
    public int getOrder() {
        return PRIORITY;
    }

    private String prepareHeaderLog(HttpHeaders httpHeaders) {
        StringBuilder headersLogBuilder = new StringBuilder(HEADERS_PREFIX);
        LOGGABLE_HEADER_KEYS.forEach(key -> {
            if (httpHeaders.containsKey(key)) {
                headersLogBuilder.append(key)
                    .append(EQUAL_SIGN)
                    .append(Objects.requireNonNull(httpHeaders.get(key)).toArray()[0])
                    .append(StringUtils.SPACE);
            }
        });

        return headersLogBuilder.toString();
    }

    private ServerHttpResponseDecorator prepareErrorHandlingResponseDecorator(ServerWebExchange exchange) {
        return new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                return DataBufferUtils.join(body)
                    .flatMap(dataBuffer -> handleError(getDelegate(), dataBuffer));
            }
        };
    }

    private Mono<Void> handleError(ServerHttpResponse response, DataBuffer dataBuffer) {
        if (response != null && dataBuffer != null) {
            if (isErrorResponseCode(response)) {
                LOGGER.error("An error with status occurred: " + response.getStatusCode());
                LOGGER.error(StandardCharsets.UTF_8.decode(dataBuffer.asByteBuffer()).toString());
            }

            return response.writeWith(Mono.just(dataBuffer));
        }

        return Mono.empty();
    }

    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    private boolean isErrorResponseCode(ServerHttpResponse response) {
        return response.getStatusCode() != null && !response.getStatusCode().is2xxSuccessful();
    }
}
