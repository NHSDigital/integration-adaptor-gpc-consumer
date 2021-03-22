package uk.nhs.adaptors.gpc.consumer.filters.uri;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

public class SspUrlBuilder {

    private static final String DIRECT_GPC_URL_PATTERN = "https://%s/%s%s";
    private static final String SDS_URL_PATTERN = "https://%s/%s/%s";

    private String sspDomain;
    private String address;
    private String initialPath;
    private String structuredFhirBaseRegex;

    public SspUrlBuilder sspDomain(String sspDomain) {
        this.sspDomain = sspDomain;
        return this;
    }

    public SspUrlBuilder address(String address) {
        this.address = address;
        return this;
    }

    public SspUrlBuilder initialPath(String initialPath) {
        this.initialPath = initialPath;
        return this;
    }

    public SspUrlBuilder structuredFhirBaseRegex(String structuredFhirBaseRegex) {
        this.structuredFhirBaseRegex = structuredFhirBaseRegex;
        return this;
    }

    public Optional<String> buildSDS() {
        String requestPath = initialPath.replaceFirst(structuredFhirBaseRegex, StringUtils.EMPTY);
        return Optional.of(String.format(SDS_URL_PATTERN, sspDomain, address, requestPath));
    }

    public Optional<String> buildDirectGPC() {
        return Optional.of(String.format(DIRECT_GPC_URL_PATTERN, sspDomain, address, initialPath));
    }
}
