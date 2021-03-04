package uk.nhs.adaptors.gpc.consumer.gpc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;
import uk.nhs.adaptors.gpc.consumer.utils.PemFormatter;

@Component
@ConfigurationProperties(prefix = "gpc-consumer.gpc")
@Getter
@Setter
public class GpcConfiguration {
    private String clientCert;
    private String clientKey;
    private String rootCA;
    private String subCA;

    public String getFormattedClientCert() {
        return PemFormatter.format(getClientCert());
    }

    public String getFormattedClientKey() {
        return PemFormatter.format(getClientKey());
    }

    public String getFormattedSubCA() {
        return PemFormatter.format(getSubCA());
    }

    public String getFormattedRootCA() {
        return PemFormatter.format(getRootCA());
    }
}
