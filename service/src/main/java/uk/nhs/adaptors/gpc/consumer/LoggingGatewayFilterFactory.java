package uk.nhs.adaptors.gpc.consumer;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
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
public class LoggingGatewayFilterFactory extends AbstractGatewayFilterFactory<LoggingGatewayFilterFactory.Config> {
    private static final List<String> LOGGABLE_HEADER_KEYS = List.of("Ssp-From", "Ssp-To");
    private static final String LOG_TEMPLATE = "Gateway filter log: %s %s URL: %s";
    private static final String HEADERS_PREFIX = "Headers: { ";
    private static final String HEADERS_SUFFIX = "}";
    private static final String COLON = ": ";
    private static final int PRIORITY = -2;

    public LoggingGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter((exchange, chain) -> prepareGatewayFilterMono(exchange, chain, config), PRIORITY);
    }

    private Mono<Void> prepareGatewayFilterMono(ServerWebExchange exchange,
            GatewayFilterChain chain,
            Config config) {
        LOGGER.info(String.format(LOG_TEMPLATE,
            config.getBaseMessage(),
            prepareHeaderLog(exchange.getRequest().getHeaders()),
            exchange.getRequest().getURI()));

        return chain.filter(exchange.mutate().response(prepareErrorHandlingResponseDecorator(exchange)).build());
    }

    private String prepareHeaderLog(HttpHeaders httpHeaders) {
        StringBuilder headersLogBuilder = new StringBuilder(HEADERS_PREFIX);
        LOGGABLE_HEADER_KEYS.forEach(key -> {
            if (httpHeaders.containsKey(key)) {
                headersLogBuilder.append(key)
                    .append(COLON)
                    .append(httpHeaders.get(key))
                    .append(StringUtils.SPACE);
            }
        });
        headersLogBuilder.append(HEADERS_SUFFIX);

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

    @Setter
    @Getter
    public static class Config {
        private String baseMessage;
    }
}
