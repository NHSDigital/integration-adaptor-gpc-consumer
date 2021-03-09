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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class FindAPatientDocsGatewayFilterFactory extends AbstractGatewayFilterFactory<FindAPatientDocsGatewayFilterFactory.Config> {

    private static final List<String> LOGGABLE_HEADER_KEYS = List.of("Ssp-From", "Ssp-To");
    private static final String LOG_TEMPLATE = "Gateway filter log: %s %s URL: %s";
    private static final String HEADERS_PREFIX = "Headers: { ";
    private static final String HEADERS_SUFFIX = "}";
    private static final String COLON = ": ";
    private static final int PRIORITY = -2;

    public FindAPatientDocsGatewayFilterFactory() {
        super(FindAPatientDocsGatewayFilterFactory.Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter((exchange, chain) -> prepareGatewayFilterMono(exchange, chain, config), PRIORITY);
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

    @SneakyThrows
    private Mono<Void> handleError(ServerHttpResponse response, DataBuffer dataBuffer, Config config) {
        if (response != null && dataBuffer != null) {
            if (isErrorResponseCode(response)) {
                LOGGER.error("An error with status occurred: " + response.getStatusCode());
                LOGGER.error(StandardCharsets.UTF_8.decode(dataBuffer.asByteBuffer()).toString());
            } else {
                var decompressedResponseString = FindAPatientDocsUtil.unzipInputStreamToString(dataBuffer.asInputStream());
                var responseWithProxyUrlReplacement = FindAPatientDocsUtil.replaceUrl(config, decompressedResponseString);
                var responseBodyGzipByteArrayOS = FindAPatientDocsUtil.zipStringToOutputStream(responseWithProxyUrlReplacement);

                DataBuffer buffer = response.bufferFactory().wrap(responseBodyGzipByteArrayOS.toByteArray());
                return response.writeWith(Mono.just(buffer));
            }
        }

        return Mono.empty();
    }

    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    private boolean isErrorResponseCode(ServerHttpResponse response) {
        return response.getStatusCode() != null && !response.getStatusCode().is2xxSuccessful();
    }

    private ServerHttpResponseDecorator prepareErrorHandlingResponseDecorator(ServerWebExchange exchange, Config config) {
        return new ServerHttpResponseDecorator(exchange.getResponse()) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                return DataBufferUtils.join(body)
                    .flatMap(dataBuffer -> handleError(getDelegate(), dataBuffer, config));
            }
        };
    }

    @Setter
    @Getter
    public static class Config {
        private String baseMessage;
        private String gpcConsumerurl;
        private String targetUrl;
    }

    private Mono<Void> prepareGatewayFilterMono(ServerWebExchange exchange, GatewayFilterChain chain, FindAPatientDocsGatewayFilterFactory.Config config) {
        LOGGER.info(String.format(LOG_TEMPLATE,
            config.getBaseMessage(),
            prepareHeaderLog(exchange.getRequest().getHeaders()),
            exchange.getRequest().getURI()));

        return chain.filter(exchange.mutate().response(prepareErrorHandlingResponseDecorator(exchange, config)).build());
    }
}
