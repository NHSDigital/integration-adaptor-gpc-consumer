package uk.nhs.adaptors.gpc.consumer.gpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.web.server.ServerWebExchange;

import lombok.SneakyThrows;
import uk.nhs.adaptors.gpc.consumer.filters.SspFilter;

@ExtendWith(MockitoExtension.class)
public class SspFilterTest {
    private static final String ORIGINAL_RELATIVE_PATH = "https://myhost.com/some/path/here";
    private static final String SSP_URL_NO_TRAILING_SLASH = "https://ssp.com";
    private static final String SSP_URL_WITH_TRAILING_SLASH = SSP_URL_NO_TRAILING_SLASH + "/";

    @Mock
    private ServerWebExchange exchange;
    @Mock
    private GatewayFilterChain chain;
    private SspFilter sspFilter;
    private Map<String, Object> attributes;
    private GpcConfiguration gpcConfiguration;

    @BeforeEach
    @SneakyThrows
    private void before() {
        gpcConfiguration = new GpcConfiguration();
        sspFilter = new SspFilter(gpcConfiguration);
        attributes = new HashMap<>();
        attributes.put(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR, new URI(ORIGINAL_RELATIVE_PATH));
        when(exchange.getAttributes()).thenReturn(attributes);
    }

    @Test
    @SneakyThrows
    public void When_SspUrlHasTrailingSlash_Expect_CorrectUrlTransformed() {
        gpcConfiguration.setSspUrl(SSP_URL_WITH_TRAILING_SLASH);

        sspFilter.filter(exchange, chain);

        assertThat(attributes.get(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR))
            .isEqualTo(new URI(SSP_URL_WITH_TRAILING_SLASH + ORIGINAL_RELATIVE_PATH));
    }

    @Test
    @SneakyThrows
    public void When_SspUrlMissingTrailingSlash_Expect_CorrectUrlTransformed() {
        gpcConfiguration.setSspUrl(SSP_URL_NO_TRAILING_SLASH);

        sspFilter.filter(exchange, chain);

        assertThat(attributes.get(ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR))
            .isEqualTo(new URI(SSP_URL_WITH_TRAILING_SLASH + ORIGINAL_RELATIVE_PATH));
    }

}
