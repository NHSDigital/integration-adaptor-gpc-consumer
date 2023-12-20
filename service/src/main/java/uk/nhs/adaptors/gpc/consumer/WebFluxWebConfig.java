package uk.nhs.adaptors.gpc.consumer;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@EnableWebFlux
@Configuration
public class WebFluxWebConfig implements WebFluxConfigurer {

    private static final int FILE_SIZE = 150 * 1024 * 1024;
    @Override
    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
        configurer.defaultCodecs().maxInMemorySize(FILE_SIZE);
    }

}
