package uk.nhs.adaptors.gpc.consumer.sds;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Device;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;

import ca.uhn.fhir.parser.IParser;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import uk.nhs.adaptors.gpc.consumer.sds.builder.SdsRequestBuilder;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Slf4j
public class SdsClient {

    private static final String NHS_MHS_ID = "https://fhir.nhs.uk/Id/nhsMHSId";
    private static final String NHS_SPINE_ASID = "https://fhir.nhs.uk/Id/nhsSpineASID";
    private final IParser fhirParser;
    private final SdsRequestBuilder sdsRequestBuilder;

    @Value("${gpc-consumer.sds.supplierOdsCode}")
    private String supplierOdsCode;

    public Mono<String> callForGetAsid(String interactionId, String fromOdsCode, String correlationId) {
        var sdsDeviceRequest = sdsRequestBuilder.buildAsDeviceAsidRequest(fromOdsCode, supplierOdsCode, interactionId, correlationId);
        return retrieveAsDeviceNhsSpineAsid(sdsDeviceRequest);
    }

    public Mono<SdsResponseData> callForGetStructuredRecord(String fromOdsCode, String correlationId) {
        var sdsDeviceRequest = sdsRequestBuilder.buildGetStructuredRecordAsDeviceRequest(fromOdsCode, correlationId);
        var sdsEndpointRequest = sdsRequestBuilder.buildGetStructuredRecordEndpointRequest(fromOdsCode, correlationId);
        return retrieveData(sdsDeviceRequest, sdsEndpointRequest);
    }

    public Mono<SdsResponseData> callForMigrateStructuredRecord(String fromOdsCode, String correlationId) {
        var sdsDeviceRequest = sdsRequestBuilder.buildMigrateStructuredRecordAsDeviceRequest(fromOdsCode, correlationId);
        var sdsEndpointRequest = sdsRequestBuilder.buildMigrateStructuredRecordEndpointRequest(fromOdsCode, correlationId);
        return retrieveData(sdsDeviceRequest, sdsEndpointRequest);
    }

    public Mono<SdsResponseData> callForPatientSearchAccessDocument(String fromOdsCode, String correlationId) {
        var sdsDeviceRequest = sdsRequestBuilder.buildPatientSearchAccessDocumentAsDeviceRequest(fromOdsCode, correlationId);
        var sdsEndpointRequest = sdsRequestBuilder.buildPatientSearchAccessDocumentEndpointRequest(fromOdsCode, correlationId);
        return retrieveData(sdsDeviceRequest, sdsEndpointRequest);
    }

    public Mono<SdsResponseData> callForSearchForDocumentRecord(String fromOdsCode, String correlationId) {
        var sdsDeviceRequest = sdsRequestBuilder.buildSearchForDocumentAsDeviceRequest(fromOdsCode, correlationId);
        var sdsEndpointRequest = sdsRequestBuilder.buildSearchForDocumentEndpointRequest(fromOdsCode, correlationId);
        return retrieveData(sdsDeviceRequest, sdsEndpointRequest);
    }

    public Mono<SdsResponseData> callForRetrieveDocumentRecord(String fromOdsCode, String correlationId) {
        var sdsDeviceRequest = sdsRequestBuilder.buildRetrieveDocumentAsDeviceRequest(fromOdsCode, correlationId);
        var sdsEndpointRequest = sdsRequestBuilder.buildRetrieveDocumentEndpointRequest(fromOdsCode, correlationId);
        return retrieveData(sdsDeviceRequest, sdsEndpointRequest);
    }

    public Mono<SdsResponseData> callForMigrateDocumentRecord(String fromOdsCode, String correlationId) {
        var sdsDeviceRequest = sdsRequestBuilder.buildMigrateDocumentAsDeviceRequest(fromOdsCode, correlationId);
        var sdsEndpointRequest = sdsRequestBuilder.buildMigrateDocumentEndpointRequest(fromOdsCode, correlationId);
        return retrieveData(sdsDeviceRequest, sdsEndpointRequest);
    }

    private Mono<SdsResponseData> retrieveData(RequestHeadersSpec<? extends RequestHeadersSpec<?>> sdsDeviceRequest,
        RequestHeadersSpec<? extends RequestHeadersSpec<?>> sdsEndpointRequest) {
        LOGGER.info("Using SDS Endpoint endpoint to retrieve GPC provider endpoint details");

        return retrieveAsDeviceNhsSpineAsid(sdsDeviceRequest)
                .flatMap(nhsSpineAsid -> performRequest(sdsEndpointRequest)
                    .map(bodyString -> fhirParser.parseResource(Bundle.class, bodyString))
                    .map(bundle -> {
                        doBundleEntryCheck(bundle);
                        var endpoint = (Endpoint) bundle.getEntryFirstRep().getResource();

                        return SdsResponseData.builder()
                                .address(getAddressFromEndpoint(endpoint))
                                .nhsMhsId(getNhsMhsId(endpoint))
                                .nhsSpineAsid(nhsSpineAsid)
                                .build();
                    })
                );
    }

    private Mono<String> retrieveAsDeviceNhsSpineAsid(RequestHeadersSpec<? extends RequestHeadersSpec<?>> request) {

        LOGGER.info("Using SDS Device endpoint to retrieve Spine ASID");

        return performRequest(request)
            .map(bodyString -> fhirParser.parseResource(Bundle.class, bodyString))
            .map(bundle -> {
                doBundleEntryCheck(bundle);
                var device = (Device) bundle.getEntryFirstRep().getResource();
                return getNhsSpineAsid(device);
            });
    }

    private String getNhsSpineAsid(Device endpoint) {
        return endpoint.getIdentifier()
            .stream()
            .filter(id -> NHS_SPINE_ASID.equals(id.getSystem()))
            .map(id -> id.getValue())
            .findFirst()
            .orElseThrow(() -> new RuntimeException(String.format("Identifier of system %s not found", NHS_SPINE_ASID)));
    }

    private String getNhsMhsId(Endpoint endpoint) {
        return endpoint.getIdentifier()
            .stream()
            .filter(id -> NHS_MHS_ID.equals(id.getSystem()))
            .map(id -> id.getValue())
            .findFirst()
            .orElseThrow(() -> new RuntimeException(String.format("Identifier of system %s not found", NHS_MHS_ID)));
    }

    private void doBundleEntryCheck(Bundle bundle) {
        LOGGER.info("Attempting to parse the bundle response from SDS");
        if (!bundle.hasEntry()) {
            throw new RuntimeException("SDS returned no result");
        }

        if (bundle.getEntry().size() > 1) {
            LOGGER.warn("SDS returned more than 1 result. Taking the first one");
        }
    }

    @NotNull
    private String getAddressFromEndpoint(Endpoint endpoint) {
        var address = endpoint.getAddress();
        if (StringUtils.isBlank(address)) {
            throw new RuntimeException("SDS returned a result but with an empty address");
        }
        LOGGER.info("Found GPC provider endpoint in SDS: {}", address);
        return address;
    }

    private Mono<String> performRequest(RequestHeadersSpec<? extends RequestHeadersSpec<?>> request) {
        return request.exchangeToMono(clientResponse -> {
            if (clientResponse.statusCode().is2xxSuccessful()) {
                return clientResponse.bodyToMono(String.class);
            }

            // Capture the status, headers and body, and allow to bubble up as a SdsPassthroughException
            HttpStatus status = (HttpStatus) clientResponse.statusCode();
            HttpHeaders headers = clientResponse.headers().asHttpHeaders();

            return clientResponse.bodyToMono(String.class)
                .defaultIfEmpty("")
                .flatMap(body -> {
                    LOGGER.warn("SDS returned error status: {}. Forwarding OperationOutcome.", status);
                    return Mono.error(new SdsPassthroughException(status, headers, body));
                });
        });
    }

    @Builder
    @Getter
    @EqualsAndHashCode
    public static class SdsResponseData {
        private final String address;
        private final String nhsMhsId;
        private final String nhsSpineAsid;
    }
}
