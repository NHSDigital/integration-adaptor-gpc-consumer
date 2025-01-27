package uk.nhs.adaptors.gpc.consumer.sds.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import uk.nhs.adaptors.gpc.consumer.sds.validation.ValidSdsConfiguration;

@Component
@ConfigurationProperties(prefix = "gpc-consumer.sds")
@Data
@Validated
@ValidSdsConfiguration
public class SdsConfiguration {
    private String url;
    private String apiKey;
    private String supplierOdsCode;
}
