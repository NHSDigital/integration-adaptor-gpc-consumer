package uk.nhs.adaptors.gpc.consumer.sds;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import ca.uhn.fhir.parser.IParser;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gpc.consumer.sds.builder.SdsRequestBuilder;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class SdsClient {
    private final IParser fhirParser;
    private final SdsRequestBuilder sdsRequestBuilder;

    public Optional<SdsResponseData> callForGetStructuredRecord(String fromOdsCode) {
        var request = sdsRequestBuilder.buildGetStructuredRecordRequest(fromOdsCode);
        return retrieveData(request);
    }

    public Optional<SdsResponseData> callForPatientSearchAccessDocument(String fromOdsCode) {
        var request = sdsRequestBuilder.buildPatientSearchAccessDocumentRequest(fromOdsCode);
        return retrieveData(request);
    }

    public Optional<SdsResponseData> callForSearchForDocumentRecord(String fromOdsCode) {
        var request = sdsRequestBuilder.buildSearchForDocumentRequest(fromOdsCode);
        return retrieveData(request);
    }

    public Optional<SdsResponseData> callForRetrieveDocumentRecord(String fromOdsCode) {
        var request = sdsRequestBuilder.buildRetrieveDocumentRequest(fromOdsCode);
        return retrieveData(request);
    }

    private Optional<SdsResponseData> retrieveData(WebClient.RequestHeadersSpec<? extends WebClient.RequestHeadersSpec<?>> request) {
        var responseBody = performRequest(request);
        var bundle = fhirParser.parseResource(Bundle.class, responseBody);

        if (!bundle.hasEntry()) {
            LOGGER.info("SDS returned no result");
            return Optional.empty();
        }

        if (bundle.getEntry().size() > 1) {
            LOGGER.warn("SDS returned more than 1 result. Taking the first one");
        }

        var endpoint = (Endpoint) bundle.getEntryFirstRep().getResource();
        var address = endpoint.getAddress();
        if (StringUtils.isBlank(address)) {
            LOGGER.warn("SDS returned a result but with an empty address");
            return Optional.empty();
        }
        return Optional.of(SdsResponseData.builder()
            .address(address)
            .build());
    }

    private String performRequest(WebClient.RequestHeadersSpec<? extends WebClient.RequestHeadersSpec<?>> request) {
        return request.retrieve()
            .bodyToMono(String.class)
            .block();
    }

    @Builder
    @EqualsAndHashCode
    public static class SdsResponseData {
        private final String address;
    }
}
