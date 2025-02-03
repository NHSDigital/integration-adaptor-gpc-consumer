package uk.nhs.adaptors.gpc.consumer.filters;

import java.security.KeyStore;
import java.util.UUID;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import com.heroku.sdk.EnvKeyStore;

import io.netty.handler.ssl.SslContext;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.adaptors.gpc.consumer.gpc.GpcConfiguration;
import uk.nhs.adaptors.gpc.consumer.utils.PemFormatter;


@Slf4j
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SslContextBuilderWrapper {

    private final GpcConfiguration config;

    @SneakyThrows
    public SslContext buildSSLContext() {
        if (config.isSslEnabled()) {
            LOGGER.info("Using SSL context with client certificates for TLS mutual authentication.");
            return buildSSLContextWithClientCertificates();
        }
        LOGGER.info("Using standard SSL context. TLS mutual authentication is not enabled.");
        return buildStandardSslContext();
    }
    @SneakyThrows
    public SslContext buildStandardSslContext() {
        var sslContext = io.netty.handler.ssl.SslContextBuilder.forClient().build();
        LOGGER.info("Built standard SSL context.");
        return sslContext;
    }

    @SneakyThrows
    private SslContext buildSSLContextWithClientCertificates() {
        var caCertChain = toPem(config.getSubCA()) + toPem(config.getRootCA());

        var randomPassword = UUID.randomUUID().toString();

        KeyStore ks = EnvKeyStore.createFromPEMStrings(
            toPem(config.getClientKey()),
            toPem(config.getClientCert()),
            randomPassword
        ).keyStore();

        KeyStore ts = EnvKeyStore.createFromPEMStrings(caCertChain, randomPassword).keyStore();

        KeyManagerFactory keyManagerFactory =
            KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(ks, randomPassword.toCharArray());

        TrustManagerFactory trustManagerFactory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(ts);

        var sslContext =  io.netty.handler.ssl.SslContextBuilder
            .forClient()
            .keyManager(keyManagerFactory)
            .trustManager(trustManagerFactory)
            .build();

        LOGGER.info("Built SSL context for TLS.");
        return sslContext;
    }

    private String toPem(String cert) {
        return PemFormatter.format(cert);
    }
}