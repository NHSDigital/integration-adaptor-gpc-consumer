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
public class FindAPatientDocsGatewayFilterFactory extends AbstractGatewayFilterFactory<FindAPatientDocsGatewayFilterFactory.Config> {
    private static final int PRIORITY = -1;
    @Value("${gpc-consumer.gpc.gpcConsumerUrl}")
    private String gpcConsumerUrl;
    @Value("${gpc-consumer.gpc.gpcUrl}")
    private String gpcUrl;
    @Value("${gpc-consumer.gpc.findPatientDocPath}")
    private String findPatientDocPath;
    public FindAPatientDocsGatewayFilterFactory() {
        super(FindAPatientDocsGatewayFilterFactory.Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter((exchange, chain) ->
            chain.filter(exchange.mutate().response(exchange.getResponse()).build()), PRIORITY);
    }

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("r1", r -> r.host("thisshouldnotwork")
                .and()
                .path("/GP0001/STU3/1/gpconnect/fhir/Patient/$gpc.getstructuredrecord")
                .uri("http://localhost:8080"))
            .route("r1", r -> r.host(gpcUrl)
                .and()
                .path("/GP0001/STU3/1/gpconnect/fhir/Patient")
                .uri("http://localhost:8080"))
            .route("FindAPatientDocs", r -> r.path(findPatientDocPath)
                .filters(f -> f.modifyResponseBody(String.class, String.class,
                    (exchange, s) -> handleResponse(s))).uri(gpcUrl + findPatientDocPath))
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
