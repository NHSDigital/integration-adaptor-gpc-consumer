package uk.nhs.adaptors.gpc.consumer.filters;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import uk.nhs.adaptors.gpc.consumer.utils.FindAPatientDocsUtil;

@Component
@Slf4j
public class SearchForDocumentsGatewayFilterFactory extends AbstractGatewayFilterFactory<SearchForDocumentsGatewayFilterFactory.Config> {
    private static final int PRIORITY = -1;
    @Value("${gpc-consumer.gpc.gpcConsumerUrl}")
    private String gpcConsumerUrl;
    @Value("${gpc-consumer.gpc.gpcUrl}")
    private String gpcUrl;
    @Value("${gpc-consumer.gpc.findSearchDocumentsPath}")
    private String findSearchDocumentsPath;
    @Value("${gpc-consumer.gpc.structuredPath}")
    private String structuredPath;
    @Value("${gpc-consumer.gpc.findPatientPath}")
    private String findPatientPath;


    public SearchForDocumentsGatewayFilterFactory() {
        super(SearchForDocumentsGatewayFilterFactory.Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter((exchange, chain) ->
            chain.filter(exchange.mutate().response(exchange.getResponse()).build()), PRIORITY);
    }

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("get-structured-record", r -> r.host(gpcUrl)
                .and()
                .path(structuredPath)
                .uri(gpcUrl))
            .route("find-a-patient", r -> r.host(gpcUrl)
                .and()
                .path(findPatientPath)
                .uri(gpcUrl))
            .route("search-documents", r -> r.path(findSearchDocumentsPath)
                .filters(f -> f.modifyResponseBody(String.class, String.class,
                    (exchange, s) -> handleResponse(s))).uri(gpcUrl + findSearchDocumentsPath))
            .build();
    }

    @SneakyThrows
    private Mono<String> handleResponse(String responseBody) {
        if (responseBody.isBlank()) {
            LOGGER.error("An error with status occurred");
            return Mono.empty();
        } else {
            return Mono.just(FindAPatientDocsUtil.replaceUrl(gpcConsumerUrl, gpcUrl, responseBody));
        }
    }

    @Setter
    @Getter
    public static class Config {
    }
}
