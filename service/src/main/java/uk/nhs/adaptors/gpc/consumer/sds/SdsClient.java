package uk.nhs.adaptors.gpc.consumer.sds;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.hl7.fhir.dstu3.model.Identifier;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;

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
    private final IParser fhirParser;
    private final SdsRequestBuilder sdsRequestBuilder;

    public Mono<SdsResponseData> callForGetStructuredRecord(String fromOdsCode, String correlationId, ServerWebExchange exchange) {
        var request = sdsRequestBuilder.buildGetStructuredRecordRequest(fromOdsCode, correlationId);
        return retrieveData(request, exchange);
    }

    public Mono<SdsResponseData> callForMigrateStructuredRecord(String fromOdsCode, String correlationId, ServerWebExchange exchange) {
        var request = sdsRequestBuilder.buildMigrateStructuredRecordRequest(fromOdsCode, correlationId);
        return retrieveData(request, exchange);
    }

    public Mono<SdsResponseData> callForPatientSearchAccessDocument(String fromOdsCode, String correlationId,
        ServerWebExchange exchange) {
        var request = sdsRequestBuilder.buildPatientSearchAccessDocumentRequest(fromOdsCode, correlationId);
        return retrieveData(request, exchange);
    }

    public Mono<SdsResponseData> callForSearchForDocumentRecord(String fromOdsCode, String correlationId, ServerWebExchange exchange) {
        var request = sdsRequestBuilder.buildSearchForDocumentRequest(fromOdsCode, correlationId);
        return retrieveData(request, exchange);
    }

    public Mono<SdsResponseData> callForRetrieveDocumentRecord(String fromOdsCode, String correlationId, ServerWebExchange exchange) {
        var request = sdsRequestBuilder.buildRetrieveDocumentRequest(fromOdsCode, correlationId);
        return retrieveData(request, exchange);
    }

    public Mono<SdsResponseData> callForMigrateDocumentRecord(String fromOdsCode, String correlationId, ServerWebExchange exchange) {
        var request = sdsRequestBuilder.buildMigrateDocumentRequest(fromOdsCode, correlationId);
        return retrieveData(request, exchange);
    }

    private Mono<SdsResponseData> retrieveData(WebClient.RequestHeadersSpec<? extends WebClient.RequestHeadersSpec<?>> request,
        ServerWebExchange exchange) {
        LOGGER.info("Using SDS to determine GPC provider endpoint");
        return performRequest(request)
            .map(bodyString -> fhirParser.parseResource(Bundle.class, bodyString))
            .map(bundle -> {
                doBundleEntryCheck(bundle);
                var endpoint = (Endpoint) bundle.getEntryFirstRep().getResource();

                return SdsResponseData.builder()
                    .address(getAddressFromEndpoint(endpoint))
                    .nhsMhsId(getNhsMhsId(endpoint))
                    .build();
            });
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

    private Mono<String> performRequest(WebClient.RequestHeadersSpec<? extends WebClient.RequestHeadersSpec<?>> request) {
        return request.retrieve()
            .bodyToMono(String.class);
    }

    @Builder
    @Getter
    @EqualsAndHashCode
    public static class SdsResponseData {
        private final String address;
        private final String nhsMhsId;
    }
}
