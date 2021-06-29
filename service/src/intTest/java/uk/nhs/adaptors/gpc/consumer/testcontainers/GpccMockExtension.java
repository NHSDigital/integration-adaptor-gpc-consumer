package uk.nhs.adaptors.gpc.consumer.testcontainers;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GpccMockExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        GpccMocksContainer.getInstance().start();
    }
}
