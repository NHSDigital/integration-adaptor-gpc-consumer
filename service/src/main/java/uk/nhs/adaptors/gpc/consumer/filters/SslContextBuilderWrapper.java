package uk.nhs.adaptors.gpc.consumer.filters;

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.UUID;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import org.apache.commons.lang3.StringUtils;

import com.heroku.sdk.EnvKeyStore;

import io.netty.handler.ssl.SslContext;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gpc.consumer.gpc.exception.GpConnectException;
import uk.nhs.adaptors.gpc.consumer.utils.PemFormatter;

@Slf4j
public class SslContextBuilderWrapper {
    private String clientKey;
    private String clientCert;
    private String subAndRootCert;

    public SslContextBuilderWrapper clientKey(String clientKey) {
        LOGGER.info(String.format("DEPLOYMENT DEBUGGING, key:'%s'", clientKey));
        this.clientKey = toPem(clientKey);
        return this;
    }

    public SslContextBuilderWrapper clientCert(String clientCert) {
        LOGGER.info(String.format("DEPLOYMENT DEBUGGING, cert:'%s'", clientCert));
        this.clientCert = toPem(clientCert);
        return this;
    }

    public SslContextBuilderWrapper subAndRootCert(String subAndRootCert) {
        LOGGER.info(String.format("DEPLOYMENT DEBUGGING, subAndRootCert:'%s'", subAndRootCert));
        this.subAndRootCert = toPem(subAndRootCert);
        return this;
    }

    @SneakyThrows
    public SslContext buildSSLContext() {
        if (shouldBuildSslContext()) {
            LOGGER.info("Using SSL context with client certificates for TLS mutual authentication.");
            return buildSSLContextWithClientCertificates();
        }
        LOGGER.info("Using standard SSL context. TLS mutual authentication is not enabled.");
        return io.netty.handler.ssl.SslContextBuilder.forClient().build();
    }

    private boolean shouldBuildSslContext() {
        final int allSslProperties = 3;

        var missingSslProperties = new ArrayList<String>();
        if (StringUtils.isBlank(clientKey)) {
            missingSslProperties.add("GPC_CONSUMER_SPINE_CLIENT_KEY");
        }
        if (StringUtils.isBlank(clientCert)) {
            missingSslProperties.add("GPC_CONSUMER_SPINE_CLIENT_CERT");
        }
        if (StringUtils.isBlank(subAndRootCert)) {
            missingSslProperties.add("GPC_CONSUMER_SPINE_SUB_AND_ROOT_CA_CERT");
        }

        if (missingSslProperties.size() == allSslProperties) {
            LOGGER.debug("No TLS MA properties were provided. Not configuring an SSL context.");
            return false;
        } else if (missingSslProperties.isEmpty()) {
            LOGGER.debug("All TLS MA properties were provided. Configuration an SSL context.");
            return true;
        } else {
            throw new GpConnectException("All or none of the GPC_CONSUMER_SPINE_ variables must be defined. Missing variables: "
                + String.join(",", missingSslProperties));
        }
    }

    @SneakyThrows
    private SslContext buildSSLContextWithClientCertificates() {
        var randomPassword = UUID.randomUUID().toString();

        KeyStore ks = EnvKeyStore.createFromPEMStrings(
            clientKey, clientCert,
            randomPassword).keyStore();

        KeyStore ts = EnvKeyStore.createFromPEMStrings(subAndRootCert, randomPassword).keyStore();

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
        return StringUtils.isNotBlank(cert) ? PemFormatter.format(cert) : StringUtils.EMPTY;
    }
}