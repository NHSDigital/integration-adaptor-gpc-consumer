package uk.nhs.adaptors.gpc.consumer.filters;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.cloud.gateway.filter.NettyRoutingFilter;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.netty.handler.ssl.SslContext;
import reactor.netty.http.client.HttpClient;
import uk.nhs.adaptors.gpc.consumer.utils.PemFormatter;

@Component
public class SspRoutingFiler extends NettyRoutingFilter {
    @Value("${gpc-consumer.gpc.clientKey}")
    private String clientKey;
    @Value("${gpc-consumer.gpc.clientCert}")
    private String clientCert;
    @Value("${gpc-consumer.gpc.rootCA}")
    private String rootCA;
    @Value("${gpc-consumer.gpc.subCA}")
    private String subCA;
    @Value("${gpc-consumer.gpc.sspEnabled}")
    private String sspEnabled;

    public SspRoutingFiler(
        HttpClient httpClient,
        ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider,
        HttpClientProperties properties) {
        super(httpClient, headersFiltersProvider, properties);
    }

    @Override
    protected HttpClient getHttpClient(Route route, ServerWebExchange exchange) {
        if (sspEnabled.equals("true")) {
            String clientKey = PemFormatter.format(this.clientKey);
            String clientCert = PemFormatter.format(this.clientCert);
            String rootCA = PemFormatter.format(this.rootCA);
            String subCA = PemFormatter.format(this.subCA);
            SslContext sslContext = new SslContextBuilderWrapper(clientKey, clientCert, rootCA, subCA).buildSSLContext();
            return HttpClient.create().secure(t -> t.sslContext(sslContext));
        }
        return HttpClient.create();
    }
}
