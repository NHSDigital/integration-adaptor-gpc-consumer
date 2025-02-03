package uk.nhs.adaptors.gpc.consumer.gpc;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import uk.nhs.adaptors.gpc.consumer.filters.SslContextBuilderWrapper;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
public class SslContextBuilderWrapperTest {

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


    @Test
    void When_BuildSslContextAndTlsIsEnabled_Expect_TLSEnabledSSLContextIsBuilt(CapturedOutput capturedOutput) {
        var gpcConfiguration = new GpcConfiguration();
        gpcConfiguration.setClientCert(VALID_CERTIFICATE);
        gpcConfiguration.setClientKey(VALID_RSA_PRIVATE_KEY);
        gpcConfiguration.setRootCA(VALID_CERTIFICATE);
        gpcConfiguration.setSubCA(VALID_CERTIFICATE);
        gpcConfiguration.setSslEnabled(true);

        new SslContextBuilderWrapper(gpcConfiguration).buildSSLContext();

        assertThat(capturedOutput.getOut()).contains("Built SSL context for TLS.");
    }

    @Test
    void When_BuildSslContextAndTlsIsNotEnabled_Expect_StandardSSLContextIsBuilt(CapturedOutput capturedOutput) {
        var gpcConfiguration = new GpcConfiguration();
        gpcConfiguration.setSslEnabled(false);

        new SslContextBuilderWrapper(gpcConfiguration).buildSSLContext();

        assertThat(capturedOutput.getOut()).contains("Built standard SSL context.");
    }
}