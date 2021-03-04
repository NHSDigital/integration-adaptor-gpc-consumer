package uk.nhs.adaptors.gpc.consumer.sds.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "gpc-consumer.sds")
@Getter
@Setter
public class SdsConfiguration {
    private String url;
    private String apiKey;
}
