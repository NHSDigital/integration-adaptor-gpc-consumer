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
    private static final String PLACEHOLDER_URI = "http://0.0.0.0";
    @Value("${gpc-consumer.gpc.searchForAPatientsDocumentsPath}")
    private String searchForAPatientsDocumentsPath;
    @Value("${gpc-consumer.gpc.structuredPath}")
    private String structuredPath;
    @Value("${gpc-consumer.gpc.findPatientPath}")
    private String findPatientPath;
    @Value("${gpc-consumer.gpc.documentPath}")
    private String documentPath;
    @Value("${gpc-consumer.gpc.migrateStructuredPath}")
    private String migrateStructuredPath;

    @Value("${gpc-consumer.sds.enableSDS}")
    private String enableSds;
    @Value("${gpc-consumer.gpc.overrideGpcProviderUrl}")
    private String overrideGpcProviderUrl;

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
                .uri(getDefaultTargetUri()))
            .route("get-structured-record", r -> r.path(structuredPath)
                .filters(f -> f.modifyResponseBody(String.class, String.class, urlsInResponseBodyRewriteFunction))
                .uri(getDefaultTargetUri()))
            .route("find-a-patient", r -> r.path(findPatientPath)
                .and()
                .uri(getDefaultTargetUri()))
            .route("migrate-structured-record", r -> r.path(migrateStructuredPath)
                .and()
                .uri(getDefaultTargetUri()))
            .route("search-documents", r -> r.path(searchForAPatientsDocumentsPath)
                .filters(f -> f.modifyResponseBody(String.class, String.class, urlsInResponseBodyRewriteFunction))
                .uri(getDefaultTargetUri()))
            .build();
    }

    private String getDefaultTargetUri() {
        if (Boolean.parseBoolean(enableSds)) {
            LOGGER.info("SDS is enabled. Configuring proxy paths to target an unrouteable placeholder address. "
                + "SdsFilter will replace this target URI at runtime.");
            return PLACEHOLDER_URI;
        } else {
            LOGGER.warn("SDS is disabled. Configuring proxy paths to target the override GPC provider URL. The SdsFilter will not run.");
            return overrideGpcProviderUrl;
        }
    }

    @Setter
    @Getter
    public static class Config {
    }
}
