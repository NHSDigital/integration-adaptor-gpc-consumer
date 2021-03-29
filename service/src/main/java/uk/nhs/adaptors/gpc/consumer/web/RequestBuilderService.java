package uk.nhs.adaptors.gpc.consumer.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

import io.netty.handler.ssl.SslContext;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gpc.consumer.filters.SslContextBuilderWrapper;
import uk.nhs.adaptors.gpc.consumer.gpc.GpcConfiguration;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class RequestBuilderService {
    private static final int BYTE_COUNT = 16 * 1024 * 1024;

    private final GpcConfiguration gpcConfiguration;

    @SneakyThrows
    public SslContext buildSSLContext() {
        return new SslContextBuilderWrapper()
            .clientKey(gpcConfiguration.getClientKey())
            .clientCert(gpcConfiguration.getClientCert())
            .rootCert(gpcConfiguration.getRootCA())
            .subCert(gpcConfiguration.getSubCA())
            .buildSSLContext();
    }

    public ExchangeStrategies buildExchangeStrategies() {
        return ExchangeStrategies
            .builder()
            .codecs(
                configurer -> configurer.defaultCodecs()
                    .maxInMemorySize(BYTE_COUNT)).build();
    }
}
