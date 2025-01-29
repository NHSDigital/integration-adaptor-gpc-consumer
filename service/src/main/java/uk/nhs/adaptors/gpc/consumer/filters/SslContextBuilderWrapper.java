package uk.nhs.adaptors.gpc.consumer.filters;

import java.security.KeyStore;
import java.util.UUID;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import com.heroku.sdk.EnvKeyStore;

import io.netty.handler.ssl.SslContext;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import uk.nhs.adaptors.gpc.consumer.utils.PemFormatter;


@Slf4j
public class SslContextBuilderWrapper {
    @Value("${gpc-consumer.gpc.clientKey}")
    private String clientKey;
    @Value("${gpc-consumer.gpc.clientCert}")
    private String clientCert;
    @Value("${gpc-consumer.gpc.rootCA}")
    private String rootCA;
    @Value("${gpc-consumer.gpc.subCA}")
    private String subCA;
    @Value("${gpc-consumer.gpc.sslEnabled}")
    private boolean sslEnabled;

    @SneakyThrows
    public SslContext buildSSLContext() {
        if (sslEnabled) {
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
        var caCertChain = toPem(subCA) + toPem(rootCA);

        var randomPassword = UUID.randomUUID().toString();

        KeyStore ks = EnvKeyStore.createFromPEMStrings(
            toPem(clientKey),
            toPem(clientCert),
            randomPassword
        ).keyStore();

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

    private String toPem(String cert) {
        return PemFormatter.format(cert);
    }
}