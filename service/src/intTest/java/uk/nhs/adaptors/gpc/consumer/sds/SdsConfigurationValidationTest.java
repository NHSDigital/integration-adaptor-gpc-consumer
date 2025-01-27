package uk.nhs.adaptors.gpc.consumer.sds;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import uk.nhs.adaptors.gpc.consumer.sds.configuration.SdsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

public class SdsConfigurationValidationTest {

    private static final String URL = "url";
    private static final String API_KEY = "apiKey";
    private static final String SUPPLIER_ODS_CODE = "supplierOdsCode";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(TestSdsConfiguration.class);

    @Test
    void When_SdsConfigurationHasAllValuesPopulated_Expect_ContextIsCreatedAndValuesAreSet() {
        contextRunner
            .withPropertyValues(
                buildPropertyValue(URL, "https://example.com"),
                buildPropertyValue(API_KEY, "api-key"),
                buildPropertyValue(SUPPLIER_ODS_CODE, "A00001")
            )
            .run(context -> {
                assertThat(context)
                    .hasNotFailed()
                    .hasSingleBean(SdsConfiguration.class);

                var sdsConfiguration = context.getBean(SdsConfiguration.class);

                assertAll(
                    () -> assertThat(sdsConfiguration.getUrl()).isNotEmpty(),
                    () -> assertThat(sdsConfiguration.getApiKey()).isNotEmpty()
                );
            });
    }

    @Test
    void When_SdsDoesNotHaveUrlPopulated_Expect_ContextIsNotCreated() {
        contextRunner
            .withPropertyValues(
                buildPropertyValue(URL, ""),
                buildPropertyValue(API_KEY, "api-key"),
                buildPropertyValue(SUPPLIER_ODS_CODE, "A00001")
            )
            .run(context -> {
                assertThat(context).hasFailed();

                var startupFailure = context.getStartupFailure();

                assertThat(startupFailure)
                    .rootCause()
                    .hasMessageContaining("The environment variable(s) GPC_CONSUMER_SDS_URL must be provided.");
            });
    }

    @Test
    void When_SdsDoesNotHaveApiKeyPopulated_Expect_ContextIsNotCreated() {
        contextRunner
            .withPropertyValues(
                buildPropertyValue(URL, "https://example.com"),
                buildPropertyValue(API_KEY, ""),
                buildPropertyValue(SUPPLIER_ODS_CODE, "A00001")
            )
            .run(context -> {
                assertThat(context).hasFailed();

                var startupFailure = context.getStartupFailure();

                assertThat(startupFailure)
                    .rootCause()
                    .hasMessageContaining("The environment variable(s) GPC_CONSUMER_SDS_APIKEY must be provided.");
            });
    }

    @Test
    void When_SdsDoesNotHaveOdsCodePopulated_Expect_ContextIsNotCreated() {
        contextRunner
            .withPropertyValues(
                buildPropertyValue(URL, "https://example.com"),
                buildPropertyValue(API_KEY, "test-api-key")
            )
            .run(context -> {
                assertThat(context).hasFailed();

                var startupFailure = context.getStartupFailure();

                assertThat(startupFailure)
                    .rootCause()
                    .hasMessageContaining("The environment variable(s) GPC_SUPPLIER_ODS_CODE must be provided.");
            });
    }

    @Contract(pure = true)
    private static @NotNull String buildPropertyValue(String propertyName, String value) {
        return String.format("gpc-consumer.sds.%s=%s", propertyName, value);
    }

    @Configuration
    @EnableConfigurationProperties(SdsConfiguration.class)
    static class TestSdsConfiguration {
    }
}


