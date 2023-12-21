package uk.nhs.adaptors.gpc.consumer.gpc;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "gpc-consumer.gpc")
@Getter
@Setter
public class GpcConfiguration {
    private String clientCert;
    private String clientKey;
    private String rootCA;
    private String subCA;
    private String structuredFhirBasePathRegex;
    private String sspUrl;
    private String maxRequestSize;
}
