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

import static uk.nhs.adaptors.gpc.consumer.gpc.InteractionIds.DOCUMENT_MIGRATE_ID;
import static uk.nhs.adaptors.gpc.consumer.gpc.InteractionIds.DOCUMENT_READ_ID;

@Configuration
@Slf4j
public class RoutingGatewayFilterFactory extends AbstractGatewayFilterFactory<RoutingGatewayFilterFactory.Config> {
    private static final int PRIORITY = -3;
    private static final String PLACEHOLDER_URI = "http://0.0.0.0";
    private static final String INTERACTION_ID_HEADER_NAME = "Ssp-InteractionID";

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
                .header(INTERACTION_ID_HEADER_NAME, DOCUMENT_READ_ID)
                .uri(PLACEHOLDER_URI))
            .route("migrate-document", r -> r.path(documentPath)
                .and()
                .header(INTERACTION_ID_HEADER_NAME, DOCUMENT_MIGRATE_ID)
                .uri(PLACEHOLDER_URI))
            .route("get-structured-record", r -> r.path(structuredPath)
                .filters(f -> f.modifyResponseBody(String.class, String.class, urlsInResponseBodyRewriteFunction))
                .uri(PLACEHOLDER_URI))
            .route("find-a-patient", r -> r.path(findPatientPath)
                .and()
                .uri(PLACEHOLDER_URI))
            .route("migrate-structured-record", r -> r.path(migrateStructuredPath)
                .filters(f -> f.modifyResponseBody(String.class, String.class, urlsInResponseBodyRewriteFunction))
                .uri(PLACEHOLDER_URI))
            .route("search-documents", r -> r.path(searchForAPatientsDocumentsPath)
                .filters(f -> f.modifyResponseBody(String.class, String.class, urlsInResponseBodyRewriteFunction))
                .uri(PLACEHOLDER_URI))
            .build();
    }

    @Setter
    @Getter
    public static class Config {
    }
}
