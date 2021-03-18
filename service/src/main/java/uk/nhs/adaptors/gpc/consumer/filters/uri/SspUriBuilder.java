package uk.nhs.adaptors.gpc.consumer.filters.uri;

import java.net.URI;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.util.UriComponentsBuilder;

public class SspUriBuilder {

    private static final String DIRECT_GPC_URL_PATTERN = "https://%s/%s%s";
    private static final String SDS_URL_PATTERN = "https://%s/%s/%s";

    private String sspDomain;
    private String address;
    private String initialPath;
    private String structuredFhirBaseRegex;

    public SspUriBuilder sspDomain(String sspDomain) {
        this.sspDomain = sspDomain;
        return this;
    }

    public SspUriBuilder address(String address) {
        this.address = address;
        return this;
    }

    public SspUriBuilder initialPath(String initialPath) {
        this.initialPath = initialPath;
        return this;
    }

    public SspUriBuilder structuredFhirBaseRegex(String structuredFhirBaseRegex) {
        this.structuredFhirBaseRegex = structuredFhirBaseRegex;
        return this;
    }

    public Optional<URI> buildSDS(ServerHttpRequest serverHttpRequest) {
        String requestPath = initialPath.replaceFirst(structuredFhirBaseRegex, StringUtils.EMPTY);
        String url = String.format(SDS_URL_PATTERN, sspDomain, address, requestPath);

        return build(serverHttpRequest, url);
    }

    public Optional<URI> buildDirectGPC(ServerHttpRequest serverHttpRequest) {
        String url = String.format(DIRECT_GPC_URL_PATTERN, sspDomain, address, initialPath);

        return build(serverHttpRequest, url);
    }

    private Optional<URI> build(ServerHttpRequest serverHttpRequest, String url) {
        URI constructedUri = UriComponentsBuilder.fromUriString(url)
            .queryParams(serverHttpRequest.getQueryParams())
            .build()
            .toUri();

        return Optional.of(constructedUri);
    }
}
