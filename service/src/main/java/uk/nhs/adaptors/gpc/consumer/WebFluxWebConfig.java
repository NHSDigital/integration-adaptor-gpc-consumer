package uk.nhs.adaptors.gpc.consumer;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gpc.consumer.gpc.GpcConfiguration;

@EnableWebFlux
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Configuration
public class WebFluxWebConfig implements WebFluxConfigurer {

    private final GpcConfiguration gpcConfiguration;

    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().maxInMemorySize(gpcConfiguration.getMaxRequestSize());
    }

}
