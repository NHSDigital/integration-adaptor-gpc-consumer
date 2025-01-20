package uk.nhs.adaptors.gpc.consumer.filters;

import java.security.KeyStore;
import java.util.UUID;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import lombok.RequiredArgsConstructor;

import com.heroku.sdk.EnvKeyStore;

import io.netty.handler.ssl.SslContext;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gpc.consumer.gpc.GpcConfiguration;

@Component
@Slf4j
@RequiredArgsConstructor
public class SslContextBuilderWrapper {

    private final GpcConfiguration gpcConfiguration;

    @SneakyThrows
    public SslContext buildSSLContext() {
        if (gpcConfiguration.isSslEnabled()) {
            LOGGER.info("Using SSL context with client certificates for TLS mutual authentication.");
            return buildSSLContextWithClientCertificates();
        }
        LOGGER.info("Using standard SSL context. TLS mutual authentication is not enabled.");
        return buildStandardSslContext();
    }
    @SneakyThrows
    public SslContext buildStandardSslContext() {
        LOGGER.info("Using standard SSL context.");
        return io.netty.handler.ssl.SslContextBuilder.forClient().build();
    }

    @SneakyThrows
    private SslContext buildSSLContextWithClientCertificates() {
        var caCertChain = gpcConfiguration.getSubCA() + gpcConfiguration.getRootCA();

        var randomPassword = UUID.randomUUID().toString();

        KeyStore ks = EnvKeyStore.createFromPEMStrings(
            gpcConfiguration.getClientKey(), gpcConfiguration.getClientCert(),
            randomPassword).keyStore();

        KeyStore ts = EnvKeyStore.createFromPEMStrings(caCertChain, randomPassword).keyStore();

        KeyManagerFactory keyManagerFactory =
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(ks, randomPassword.toCharArray());

        TrustManagerFactory trustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(ts);

        return io.netty.handler.ssl.SslContextBuilder
            .forClient()
            .keyManager(keyManagerFactory)
            .trustManager(trustManagerFactory)
            .build();
    }
}