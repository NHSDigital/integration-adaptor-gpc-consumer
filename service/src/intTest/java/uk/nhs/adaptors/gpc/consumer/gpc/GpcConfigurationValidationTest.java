package uk.nhs.adaptors.gpc.consumer.gpc;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

public class GpcConfigurationValidationTest {

    private static final String VALID_CERTIFICATE =
        """
            -----BEGIN CERTIFICATE-----
            MIIDhzCCAm+gAwIBAgIESK+5NTANBgkqhkiG9w0BAQsFADBbMScwJQYDVQQDDB5SZWdlcnkgU2Vs
            Zi1TaWduZWQgQ2VydGlmaWNhdGUxIzAhBgNVBAoMGlJlZ2VyeSwgaHR0cHM6Ly9yZWdlcnkuY29t
            MQswCQYDVQQGEwJVQTAgFw0yNTAxMjAwMDAwMDBaGA8yMTI1MDEyMDEwMzYyNVowSTEVMBMGA1UE
            AwwMdGVzdC1zc2wuY29tMSMwIQYDVQQKDBpSZWdlcnksIGh0dHBzOi8vcmVnZXJ5LmNvbTELMAkG
            A1UEBhMCVUEwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCgTdIL7LEaTBbQiAziGocB
            gGNXBHHY5f8Rjit4FS+l12VR/Q9CcZU8nTc6V2mqPyxbgS/ATxpaWTPHbNGy/1VRWij1OCOTwFB2
            dBKKFKfIPya3JCY7RYKbvEH+Nrgc0fwJ3dqQ9Cv5w8oIsuM8HS34Y9mr/KlfpWc9fLxTDTmHCf4a
            /rYdRxQZfLwg2zi4rHaURn9T/S0jZMT0rwETajgnQl5WPfDV4d3Wslkz6ohEcQnvpDfB6mRSqA+C
            iFgOBDEnREW6UsEFX/kPP9O64HAQzrMwdlPso3BiVMO/rgZ2VX709vj1Wti1hJARwZeMfg0fXLnE
            MfRvUqoYm1mxSpc5AgMBAAGjYzBhMA8GA1UdEwEB/wQFMAMBAf8wDgYDVR0PAQH/BAQDAgGGMB0G
            A1UdDgQWBBQdR0XRwEDaOUdmKm21TXPncLJDhDAfBgNVHSMEGDAWgBQdR0XRwEDaOUdmKm21TXPn
            cLJDhDANBgkqhkiG9w0BAQsFAAOCAQEAMqf4IhZEBCcJx8TiHLNDc3AMpy49xBVZ7ANgd4cKjcSa
            L4sBFuT39ARn+SvFP76sNPDCzZC+xDsDLPgd4Jwweyko8hjYk02MR7FpdzlUPbVsPvfwJ1ynAb71
            VWWmvULKmXvPkOJkqdGnB2k89SXH80wMnl6YErXX3w6N3HOFjYUdyTWZmwCJ/MOBwDyOb3i8fcY+
            td+K1KtqWBrntnTic+4y2inBgI+Wipf3a+EbwbzkgU01EgTaCykhkoh7fZfNwQ5VGY+AcyNbd0xX
            WjDDOYAag96BLBMAgvjZqFl67tL+/CNO9o4YEPZ7pg0FvI3/Xp9L3edXvvzLREbaHxCnjQ==
            -----END CERTIFICATE-----""";

    private static final String VALID_RSA_PRIVATE_KEY =
        """
            -----BEGIN RSA PRIVATE KEY-----
            MIIEowIBAAKCAQEAoE3SC+yxGkwW0IgM4hqHAYBjVwRx2OX/EY4reBUvpddlUf0P
            QnGVPJ03Oldpqj8sW4EvwE8aWlkzx2zRsv9VUVoo9Tgjk8BQdnQSihSnyD8mtyQm
            O0WCm7xB/ja4HNH8Cd3akPQr+cPKCLLjPB0t+GPZq/ypX6VnPXy8Uw05hwn+Gv62
            HUcUGXy8INs4uKx2lEZ/U/0tI2TE9K8BE2o4J0JeVj3w1eHd1rJZM+qIRHEJ76Q3
            wepkUqgPgohYDgQxJ0RFulLBBV/5Dz/TuuBwEM6zMHZT7KNwYlTDv64GdlV+9Pb4
            9VrYtYSQEcGXjH4NH1y5xDH0b1KqGJtZsUqXOQIDAQABAoIBAAVVEVucL/fz9/5P
            yD3tK/h80NEgMLlKTUXEOOXxrngRxikIBe3r4U7229Nw/O7Q0yToEzKObw36UaKc
            mA0gOTJPkXU2vNg5WXPXQJafQUWD9EG7ThpCoamUhY1zPISY541cd9zCgoP4Y0wO
            x0hEoDbW+3KhIPExi1GcSJdqpTM8sEmzDnFIXylAEYhOVgjvZLt0LhleTzF5qlmG
            ywfyFJhAhkPXe6AhONKFAsBNFxEse4fljcCijbtzauBrw7K6amd+Rp8j+bwsrAqW
            a5FLerEnVGKQAugg4oj8q1N6iuTqGWVtO/SSykLgGSEWlW15+X0qJcLDz/XCFR0+
            ZHN+ypECgYEA44rEcHAWc1MgO3KaVg5ST8TLzwwWTK+Nf0nZ4qhVksZqcxBgTNot
            y2lF9u03wGwKzn0nqSBJqMYNFNjrGQZ9MDjyflCN3oTKgsVgIKssx26rGojfO5Qh
            54RBfory/YR3zhGETeF0eW/hbneRLPpEPeGAaJCbaPLLDFJaazHxnUMCgYEAtFpJ
            SWT92HtLvEqv727/xMX6RXi0jN3l2qo6g46mMn3Vvpsmgq2hK9KrLYNPvi9Z78bo
            3rKAg2qmcu5aiAOFdiNRh9WAM78+iaJc3OZ6WpWwVUMFxxEh3iM330IiGIJsr4jz
            ridOUXRVKAoEb7spfZxRZ6QjJG6q+mOrXDD+E9MCgYAHfTC79qR2hTzhWANGY9BH
            udVvahltyrVghCC8ugee/hLQ2LAit2ecc0mPN/2GwseURkBA68Qg3uvdTMpoF3OV
            W7p3d9VDhqFXroFcceXWZokRJYIbZuO6x/qT3KTkvTBoQuFU4t+/g3Qq+5p2nYIT
            e1GLn37N9HfEXw2Ey68FGwKBgQCZF5bkPV0ZeTfFwqRrm45zKxcSB69DcEzf++Yl
            rF45uAVLggoDnX2VZIO346I6L5mpZvBfsahTZaGbJ+cjU9HjgYGAy2PDCVD9phwr
            y10LLct75KOv4kQce0q/MjUdFwFJU/h92ZGqpRRwI2i2q2pB3QJg9rx5/ZMXbqmU
            XWYfzwKBgGyCA1NLMoPKPdaKnYHkoAwi7/ytLsA7IpLCg+NkiA6B7THGh60IpH7B
            RLwFShJRFGA+z4b1WoGPcmUloSJsMI6EjZDuG1gcWAbENWrcqkMKKJH6f9X/5pIq
            frOEfuS/2Ie8Rj8PZhhFoekjQHgtzba4w4oqfo1YeBFYc6QH/QKb
            -----END RSA PRIVATE KEY-----""";

    private static final String INVALID_CERTIFICATE = "------BEGIN CERTIFICATE----- invalid value -----END CERTIFICATE-----";
    private static final String INVALID_RSA_PRIVATE_KEY = "------BEGIN RSA PRIVATE KEY ----- invalid value -----END RSA PRIVATE KEY-----";
    private static final String CLIENT_CERT = "clientCert";
    private static final String CLIENT_KEY = "clientKey";
    private static final String ROOT_CA = "rootCA";
    private static final String SUB_CA = "subCA";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(TestGpcConfiguration.class);

    @Test
    void When_GpcConfigurationContainsAllSslProperties_Expect_IsContextIsCreatedAndShouldUseSslIsTrue() {
        contextRunner
            .withPropertyValues(
                buildPropertyValue(CLIENT_CERT, VALID_CERTIFICATE),
                buildPropertyValue(CLIENT_KEY, VALID_RSA_PRIVATE_KEY),
                buildPropertyValue(ROOT_CA, VALID_CERTIFICATE),
                buildPropertyValue(SUB_CA, VALID_CERTIFICATE)
            )
            .run(context -> {
                assertThat(context)
                    .hasNotFailed()
                    .hasSingleBean(GpcConfiguration.class);

                var gpcConfiguration = context.getBean(GpcConfiguration.class);

                assertAll(
                    () -> assertThat(gpcConfiguration.getClientCert()).isEqualTo(VALID_CERTIFICATE),
                    () -> assertThat(gpcConfiguration.getClientKey()).isEqualTo(VALID_RSA_PRIVATE_KEY),
                    () -> assertThat(gpcConfiguration.getRootCA()).isEqualTo(VALID_CERTIFICATE),
                    () -> assertThat(gpcConfiguration.getSubCA()).isEqualTo(VALID_CERTIFICATE),
                    () -> assertThat(gpcConfiguration.isSslEnabled()).isTrue()
                );
            });
    }

    @Test
    void When_GpcConfigurationNoSslProperties_Expect_IsContextIsCreatedAndShouldUseSslIsFalse() {
        contextRunner
            .withPropertyValues(
                buildPropertyValue(CLIENT_CERT, ""),
                buildPropertyValue(CLIENT_KEY, ""),
                buildPropertyValue(ROOT_CA, ""),
                buildPropertyValue(SUB_CA, "")
            )
            .run(context -> {
                assertThat(context)
                    .hasNotFailed()
                    .hasSingleBean(GpcConfiguration.class);

                var gpcConfiguration = context.getBean(GpcConfiguration.class);

                assertAll(
                    () -> assertThat(gpcConfiguration.getClientCert()).isEmpty(),
                    () -> assertThat(gpcConfiguration.getClientKey()).isEmpty(),
                    () -> assertThat(gpcConfiguration.getRootCA()).isEmpty(),
                    () -> assertThat(gpcConfiguration.getSubCA()).isEmpty(),
                    () -> assertThat(gpcConfiguration.isSslEnabled()).isFalse()
                );
            });
    }

    @Test
    void When_GpcConfigurationHasSomeButNotAllSslProperties_Expect_ContextNotCreated(
    ) {
        contextRunner
            .withPropertyValues(
                buildPropertyValue(CLIENT_CERT, ""),
                buildPropertyValue(CLIENT_KEY, VALID_RSA_PRIVATE_KEY),
                buildPropertyValue(ROOT_CA, VALID_CERTIFICATE),
                buildPropertyValue(SUB_CA, VALID_CERTIFICATE)
            )
            .run(context -> {
                assertThat(context).hasFailed();
                var startupFailure = context.getStartupFailure();

                assertThat(startupFailure)
                    .rootCause()
                    .hasMessageContaining("To enable mutual TLS you must provide GPC_CONSUMER_SPINE_CLIENT_CERT environment variable(s).")
                    .hasMessageContaining(
                         "To disable mutual TLS you must remove GPC_CONSUMER_SPINE_CLIENT_KEY, GPC_CONSUMER_SPINE_ROOT_CA_CERT, "
                             + "GPC_CONSUMER_SPINE_SUB_CA_CERT environment variable(s).");
            });
    }

    @Test
    void When_GpcConfigurationHasAnInvalidCertificate_Expect_ContextIsNotCreated() {
        contextRunner
            .withPropertyValues(
                buildPropertyValue(CLIENT_CERT, INVALID_CERTIFICATE),
                buildPropertyValue(CLIENT_KEY, VALID_RSA_PRIVATE_KEY),
                buildPropertyValue(ROOT_CA, VALID_CERTIFICATE),
                buildPropertyValue(SUB_CA, VALID_CERTIFICATE)
            )
            .run(context -> {
                assertThat(context).hasFailed();

                var startupFailure = context.getStartupFailure();

                assertThat(startupFailure)
                    .rootCause()
                    .hasMessageContaining(
                        "The environment variable(s) GPC_CONSUMER_SPINE_CLIENT_CERT are not in a valid PEM format"
                    );
            });
    }

    @Test
    void When_GpcConfigurationHasAnInvalidClientKey_Expect_ContextIsNotCreated() {
        contextRunner
            .withPropertyValues(
                buildPropertyValue(CLIENT_CERT, VALID_CERTIFICATE),
                buildPropertyValue(CLIENT_KEY, INVALID_RSA_PRIVATE_KEY),
                buildPropertyValue(ROOT_CA, VALID_CERTIFICATE),
                buildPropertyValue(SUB_CA, VALID_CERTIFICATE)
            )
            .run(context -> {
                assertThat(context).hasFailed();

                var startupFailure = context.getStartupFailure();

                assertThat(startupFailure)
                    .rootCause()
                    .hasMessageContaining(
                        "The environment variable(s) GPC_CONSUMER_SPINE_CLIENT_KEY are not in a valid PEM format"
                    );
            });
    }

    @Test
    void When_GpcConfigurationHasSspUrlPresentWithTrailingSlash_Expect_ContextIsCreatedAndIsSspEnabled() {
        contextRunner
            .withPropertyValues(
                buildPropertyValue("sspUrl", "/this-is-a-url.com/")
            )
            .run(context -> {
                assertThat(context)
                    .hasNotFailed()
                    .hasSingleBean(GpcConfiguration.class);

                var gpcConfiguration = context.getBean(GpcConfiguration.class);
                assertThat(gpcConfiguration.isSspEnabled()).isTrue();
            });
    }

    @Test
    void When_GpcConfigurationHasSspUrlPresentWithoutTrailingSlash_Expect_ContextIsCreatedAndIsSspEnabledAndUrlHasTrailingSlash() {
        contextRunner
            .withPropertyValues(
                buildPropertyValue("sspUrl", "/this-is-a-url.com")
            )
            .run(context -> {
                assertThat(context)
                    .hasNotFailed()
                    .hasSingleBean(GpcConfiguration.class);

                var gpcConfiguration = context.getBean(GpcConfiguration.class);

                assertAll(
                    () -> assertThat(gpcConfiguration.isSspEnabled()).isTrue(),
                    () -> assertThat(gpcConfiguration.getSspUrl()).isEqualTo("/this-is-a-url.com/")
                );
            });
    }

    @Test
    void When_GpcConfigurationDoesNotHaveSspUrlPresent_Expect_ContextIsCreatedAndSspIsNotEnabled() {
        contextRunner
            .withPropertyValues(
                buildPropertyValue("sspUrl", "")
            )
            .run(context -> {
                assertThat(context)
                    .hasNotFailed()
                    .hasSingleBean(GpcConfiguration.class);

                var gpcConfiguration = context.getBean(GpcConfiguration.class);
                assertThat(gpcConfiguration.isSspEnabled()).isFalse();
            });
    }

    @Contract(pure = true)
    private static @NotNull String buildPropertyValue(String propertyName, String value) {
        return String.format("gpc-consumer.gpc.%s=%s", propertyName, value);
    }

    @Configuration
    @EnableConfigurationProperties(GpcConfiguration.class)
    static class TestGpcConfiguration {
    }
}


