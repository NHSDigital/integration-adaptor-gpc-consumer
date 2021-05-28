package uk.nhs.adaptors.gpc.consumer.filters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.OrderedGatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class RoutingGatewayFilterFactory extends AbstractGatewayFilterFactory<RoutingGatewayFilterFactory.Config> {
    private static final int PRIORITY = -3;
    @Value("${gpc-consumer.gpc.gpcConsumerUrl}")
    private String gpcConsumerUrl;
    @Value("${gpc-consumer.gpc.gpcUrl}")
    private String gpcUrl;
    @Value("${gpc-consumer.gpc.searchForAPatientsDocumentsPath}")
    private String searchForAPatientsDocumentsPath;
    @Value("${gpc-consumer.gpc.structuredPath}")
    private String structuredPath;
    @Value("${gpc-consumer.gpc.findPatientPath}")
    private String findPatientPath;
    @Value("${gpc-consumer.gpc.documentPath}")
    private String documentPath;

    @Autowired
    private UrlsInResponseBodyRewriteFunction urlsInResponseBodyRewriteFunction;

    @Override
    public GatewayFilter apply(Config config) {
        return new OrderedGatewayFilter((exchange, chain) ->
            chain.filter(exchange.mutate().response(exchange.getResponse()).build()), PRIORITY);
    }

    @Bean
    public RouteLocator routes(RouteLocatorBuilder builder) {
        return builder.routes()
            .route("get-document", r -> r.path(documentPath)
                .and()
                .uri(gpcUrl))
            .route("get-structured-record", r -> r.path(structuredPath)
                .and()
                .uri(gpcUrl))
            .route("find-a-patient", r -> r.path(findPatientPath)
                .and()
                .uri(gpcUrl))
            .route("search-documents", r -> r.path(searchForAPatientsDocumentsPath)
                .filters(f -> f.modifyResponseBody(String.class, String.class, urlsInResponseBodyRewriteFunction))
                .uri(gpcUrl))
            .build();
    }

    @Setter
    @Getter
    public static class Config {
    }
}
