package uk.nhs.adaptors.gpc.consumer.filters;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.cloud.gateway.filter.NettyRoutingFilter;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.netty.handler.ssl.SslContext;
import reactor.netty.http.client.HttpClient;

@Component
public class TlsMutualAuthRoutingFilter extends NettyRoutingFilter {

    private final SslContextBuilderWrapper sslContextBuilderWrapper;

    public TlsMutualAuthRoutingFilter(
        HttpClient httpClient,
        ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider,
        HttpClientProperties properties,
        SslContextBuilderWrapper sslContextBuilderWrapper) {
        super(httpClient, headersFiltersProvider, properties);
        this.sslContextBuilderWrapper = sslContextBuilderWrapper;
    }

    @Override
    protected HttpClient getHttpClient(Route route, ServerWebExchange exchange) {
        SslContext sslContext = sslContextBuilderWrapper.buildSSLContext();
        return HttpClient.create().secure(t -> t.sslContext(sslContext));
    }
}