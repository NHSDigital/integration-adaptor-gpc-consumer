package uk.nhs.adaptors.gpc.consumer.sds;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Device;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    private static final String LOOKUP_CONTEXT_PROVIDER_DEVICE_ASID = "provider-device-asid";
    private static final String LOOKUP_CONTEXT_PROVIDER_ENDPOINT = "provider-endpoint";
    private static final String LOOKUP_CONTEXT_CONSUMER_ASID = "consumer-asid";
    private final IParser fhirParser;
    private final SdsRequestBuilder sdsRequestBuilder;

    @Value("${gpc-consumer.sds.supplierOdsCode}")
    private String supplierOdsCode;

    public Mono<String> callForGetAsid(String interactionId, String fromOdsCode, String correlationId) {
        LOGGER.info("SDS lookup for consumer ASID (fromOdsCode={}, interactionId={}, correlationId={})",
            fromOdsCode, interactionId, correlationId);
        var sdsDeviceRequest = sdsRequestBuilder.buildAsDeviceAsidRequest(fromOdsCode, supplierOdsCode, interactionId, correlationId);
        return retrieveAsDeviceNhsSpineAsid(sdsDeviceRequest, LOOKUP_CONTEXT_CONSUMER_ASID);
    }

    public Mono<SdsResponseData> callForGetStructuredRecord(String fromOdsCode, String correlationId) {
        LOGGER.info("SDS lookup for GetStructuredRecord (fromOdsCode={}, correlationId={})", fromOdsCode, correlationId);
        var sdsDeviceRequest = sdsRequestBuilder.buildGetStructuredRecordAsDeviceRequest(fromOdsCode, correlationId);
        var sdsEndpointRequest = sdsRequestBuilder.buildGetStructuredRecordEndpointRequest(fromOdsCode, correlationId);
        return retrieveData(sdsDeviceRequest, sdsEndpointRequest);
    }

    public Mono<SdsResponseData> callForMigrateStructuredRecord(String fromOdsCode, String correlationId) {
        LOGGER.info("SDS lookup for MigrateStructuredRecord (fromOdsCode={}, correlationId={})", fromOdsCode, correlationId);
        var sdsDeviceRequest = sdsRequestBuilder.buildMigrateStructuredRecordAsDeviceRequest(fromOdsCode, correlationId);
        var sdsEndpointRequest = sdsRequestBuilder.buildMigrateStructuredRecordEndpointRequest(fromOdsCode, correlationId);
        return retrieveData(sdsDeviceRequest, sdsEndpointRequest);
    }

    public Mono<SdsResponseData> callForPatientSearchAccessDocument(String fromOdsCode, String correlationId) {
        LOGGER.info("SDS lookup for PatientSearchAccessDocument (fromOdsCode={}, correlationId={})", fromOdsCode, correlationId);
        var sdsDeviceRequest = sdsRequestBuilder.buildPatientSearchAccessDocumentAsDeviceRequest(fromOdsCode, correlationId);
        var sdsEndpointRequest = sdsRequestBuilder.buildPatientSearchAccessDocumentEndpointRequest(fromOdsCode, correlationId);
        return retrieveData(sdsDeviceRequest, sdsEndpointRequest);
    }

    public Mono<SdsResponseData> callForSearchForDocumentRecord(String fromOdsCode, String correlationId) {
        LOGGER.info("SDS lookup for SearchForDocument (fromOdsCode={}, correlationId={})", fromOdsCode, correlationId);
        var sdsDeviceRequest = sdsRequestBuilder.buildSearchForDocumentAsDeviceRequest(fromOdsCode, correlationId);
        var sdsEndpointRequest = sdsRequestBuilder.buildSearchForDocumentEndpointRequest(fromOdsCode, correlationId);
        return retrieveData(sdsDeviceRequest, sdsEndpointRequest);
    }

    public Mono<SdsResponseData> callForRetrieveDocumentRecord(String fromOdsCode, String correlationId) {
        LOGGER.info("SDS lookup for RetrieveDocument (fromOdsCode={}, correlationId={})", fromOdsCode, correlationId);
        var sdsDeviceRequest = sdsRequestBuilder.buildRetrieveDocumentAsDeviceRequest(fromOdsCode, correlationId);
        var sdsEndpointRequest = sdsRequestBuilder.buildRetrieveDocumentEndpointRequest(fromOdsCode, correlationId);
        return retrieveData(sdsDeviceRequest, sdsEndpointRequest);
    }

    public Mono<SdsResponseData> callForMigrateDocumentRecord(String fromOdsCode, String correlationId) {
        LOGGER.info("SDS lookup for MigrateDocument (fromOdsCode={}, correlationId={})", fromOdsCode, correlationId);
        var sdsDeviceRequest = sdsRequestBuilder.buildMigrateDocumentAsDeviceRequest(fromOdsCode, correlationId);
        var sdsEndpointRequest = sdsRequestBuilder.buildMigrateDocumentEndpointRequest(fromOdsCode, correlationId);
        return retrieveData(sdsDeviceRequest, sdsEndpointRequest);
    }

    private Mono<SdsResponseData> retrieveData(RequestHeadersSpec<? extends RequestHeadersSpec<?>> sdsDeviceRequest,
        RequestHeadersSpec<? extends RequestHeadersSpec<?>> sdsEndpointRequest) {
        LOGGER.info("Using SDS Endpoint endpoint to retrieve GPC provider endpoint details");

        return retrieveAsDeviceNhsSpineAsid(sdsDeviceRequest, LOOKUP_CONTEXT_PROVIDER_DEVICE_ASID)
                .flatMap(nhsSpineAsid -> performRequest(sdsEndpointRequest)
                    .map(bodyString -> fhirParser.parseResource(Bundle.class, bodyString))
                    .map(bundle -> {
                        doBundleEntryCheck(bundle, LOOKUP_CONTEXT_PROVIDER_ENDPOINT);
                        var endpoint = (Endpoint) bundle.getEntryFirstRep().getResource();
                        var nhsMhsId = getNhsMhsId(endpoint);
                        var address = getAddressFromEndpoint(endpoint);

                        LOGGER.info("SDS provider details retrieved (nhsMhsId={}, nhsSpineAsid={}, address={})",
                            nhsMhsId, nhsSpineAsid, address);

                        return SdsResponseData.builder()
                                .address(address)
                                .nhsMhsId(nhsMhsId)
                                .nhsSpineAsid(nhsSpineAsid)
                                .build();
                    })
                )
                .doOnError(error -> LOGGER.error("Failed to retrieve SDS provider endpoint details", error));
    }

    private Mono<String> retrieveAsDeviceNhsSpineAsid(RequestHeadersSpec<? extends RequestHeadersSpec<?>> request,
        String lookupContext) {

        LOGGER.info("Using SDS Device endpoint to retrieve Spine ASID for {} lookup", lookupContext);

        return performRequest(request)
            .doOnNext(bodyString -> LOGGER.info("Received SDS Device response for {} lookup (payloadLength={})",
                lookupContext, bodyString.length()))
            .map(bodyString -> fhirParser.parseResource(Bundle.class, bodyString))
            .map(bundle -> {
                doBundleEntryCheck(bundle, lookupContext);
                var device = (Device) bundle.getEntryFirstRep().getResource();
                return getNhsSpineAsid(device);
            })
            .doOnError(error -> LOGGER.error("Failed to retrieve SDS Spine ASID for {} lookup", lookupContext, error));
    }

    private String getNhsSpineAsid(Device endpoint) {
        return endpoint.getIdentifier()
            .stream()
            .filter(id -> NHS_SPINE_ASID.equals(id.getSystem()))
            .map(id -> id.getValue())
            .findFirst()
            .orElseThrow(() -> {
                LOGGER.error("SDS Device response is missing identifier system {}", NHS_SPINE_ASID);
                return new RuntimeException(String.format("Identifier of system %s not found", NHS_SPINE_ASID));
            });
    }

    private String getNhsMhsId(Endpoint endpoint) {
        return endpoint.getIdentifier()
            .stream()
            .filter(id -> NHS_MHS_ID.equals(id.getSystem()))
            .map(id -> id.getValue())
            .findFirst()
            .orElseThrow(() -> {
                LOGGER.error("SDS Endpoint response is missing identifier system {}", NHS_MHS_ID);
                return new RuntimeException(String.format("Identifier of system %s not found", NHS_MHS_ID));
            });
    }

    private void doBundleEntryCheck(Bundle bundle, String lookupContext) {
        LOGGER.info("Attempting to parse the bundle response from SDS ({})", getBundleSummary(bundle, lookupContext));
        if (!bundle.hasEntry()) {
            LOGGER.error("SDS returned no entries ({})", getBundleSummary(bundle, lookupContext));
            throw new RuntimeException(String.format("SDS returned no result (%s)", getBundleSummary(bundle, lookupContext)));
        }

        if (bundle.getEntry().size() > 1) {
            LOGGER.warn("SDS returned more than 1 result. Taking the first one ({})", getBundleSummary(bundle, lookupContext));
        }
    }

    private String getBundleSummary(Bundle bundle, String lookupContext) {
        return String.format("lookupContext=%s, bundleType=%s, bundleTotal=%d, entryCount=%d",
            lookupContext,
            bundle.getType(),
            bundle.getTotal(),
            bundle.getEntry().size());
    }

    @NotNull
    private String getAddressFromEndpoint(Endpoint endpoint) {
        var address = endpoint.getAddress();
        if (StringUtils.isBlank(address)) {
            LOGGER.error("SDS Endpoint response contained an empty address");
            throw new RuntimeException("SDS returned a result but with an empty address");
        }
        return address;
    }

    private Mono<String> performRequest(RequestHeadersSpec<? extends RequestHeadersSpec<?>> request) {
        return request.retrieve()
            .bodyToMono(String.class)
            .doOnError(e -> LOGGER.error("SDS request failed", e));
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
