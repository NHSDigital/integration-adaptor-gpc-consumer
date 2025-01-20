package uk.nhs.adaptors.gpc.consumer.gpc;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import uk.nhs.adaptors.gpc.consumer.gpc.validation.ValidGpcConfiguration;

@Component
@ConfigurationProperties(prefix = "gpc-consumer.gpc")
@Data
@Validated
@ValidGpcConfiguration
public class GpcConfiguration {
    private String clientCert;
    private String clientKey;
    private String rootCA;
    private String subCA;
    private String sspUrl;

    private boolean sslEnabled;
    private boolean sspEnabled;
}
