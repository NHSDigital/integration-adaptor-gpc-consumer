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
    private static final String URL_VALUE = "https://example.com";
    private static final String API_KEY = "apiKey";
    private static final String API_KEY_VALUE = "api-key";
    private static final String SUPPLIER_ODS_CODE = "supplierOdsCode";
    private static final String SUPPLIER_ODS_CODE_VALUE = "A00001";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(TestSdsConfiguration.class);

    @Test
    void When_SdsConfigurationHasAllValuesPopulated_Expect_ContextIsCreatedAndValuesAreSet() {
        contextRunner
            .withPropertyValues(
                buildPropertyValue(URL, URL_VALUE),
                buildPropertyValue(API_KEY, API_KEY_VALUE),
                buildPropertyValue(SUPPLIER_ODS_CODE, SUPPLIER_ODS_CODE_VALUE)

            )
            .run(context -> {
                assertThat(context)
                    .hasNotFailed()
                    .hasSingleBean(SdsConfiguration.class);

                var sdsConfiguration = context.getBean(SdsConfiguration.class);

                assertAll(
                    () -> assertThat(sdsConfiguration.getUrl()).isEqualTo(URL_VALUE),
                    () -> assertThat(sdsConfiguration.getApiKey()).isEqualTo(API_KEY_VALUE),
                    () -> assertThat(sdsConfiguration.getSupplierOdsCode()).isEqualTo(SUPPLIER_ODS_CODE_VALUE)
                );
            });
    }

    @Test
    void When_SdsConfigurationDoesNotHaveUrlPopulated_Expect_ContextIsNotCreated() {
        contextRunner
            .withPropertyValues(
                buildPropertyValue(URL, ""),
                buildPropertyValue(API_KEY, API_KEY_VALUE),
                buildPropertyValue(SUPPLIER_ODS_CODE, SUPPLIER_ODS_CODE_VALUE)
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
    void When_SdsConfigurationDoesNotHaveApiKeyPopulated_Expect_ContextIsNotCreated() {
        contextRunner
            .withPropertyValues(
                buildPropertyValue(URL, URL_VALUE),
                buildPropertyValue(API_KEY, ""),
                buildPropertyValue(SUPPLIER_ODS_CODE, SUPPLIER_ODS_CODE_VALUE)
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
    void When_SdsConfigurationDoesNotHaveOdsCodePopulated_Expect_ContextIsNotCreated() {
        contextRunner
            .withPropertyValues(
                buildPropertyValue(URL, URL_VALUE),
                buildPropertyValue(API_KEY, API_KEY_VALUE)
            )
            .run(context -> {
                assertThat(context).hasFailed();

                var startupFailure = context.getStartupFailure();

                assertThat(startupFailure)
                    .rootCause()
                    .hasMessageContaining("The environment variable(s) GPC_SUPPLIER_ODS_CODE must be provided.");
            });
    }

    @Test
    void When_SdsConfigurationHasMultipleValuesNotPopulated_Expect_ContextIsNotCreated() {
        contextRunner
            .withPropertyValues(
                buildPropertyValue(URL, URL_VALUE)
            )
            .run(context -> {
                assertThat(context).hasFailed();

                var startupFailure = context.getStartupFailure();

                assertThat(startupFailure)
                    .rootCause()
                    .hasMessageContaining("The environment variable(s) GPC_CONSUMER_SDS_APIKEY, GPC_SUPPLIER_ODS_CODE must be provided.");
            });
    }

    @Test
    void When_SdsConfigurationDoesNotHaveAnyValuesPopulated_Expect_ContextIsNotCreated() {
        contextRunner
            .run(context -> {
                assertThat(context).hasFailed();

                var startupFailure = context.getStartupFailure();

                assertThat(startupFailure)
                    .rootCause()
                    .hasMessageContaining(
                        "The environment variable(s) GPC_CONSUMER_SDS_URL, GPC_CONSUMER_SDS_APIKEY, GPC_SUPPLIER_ODS_CODE must be provided.");
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