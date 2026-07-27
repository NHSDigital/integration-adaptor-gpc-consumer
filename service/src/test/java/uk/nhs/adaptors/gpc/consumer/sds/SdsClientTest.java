package uk.nhs.adaptors.gpc.consumer.sds;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Device;
import org.hl7.fhir.dstu3.model.Endpoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.nhs.adaptors.gpc.consumer.sds.builder.SdsRequestBuilder;
import static org.junit.Assert.assertEquals;

@ExtendWith(MockitoExtension.class)
class SdsClientTest {

    private static final String FROM_ODS_CODE = "A12345";
    private static final String CORRELATION_ID = "corr-id-001";
    private static final String SUPPLIER_ODS_CODE = "SUPPLIER01";
    private static final String INTERACTION_ID = "urn:nhs:names:services:gpconnect:fhir:operation:gpc.getstructuredrecord-1";
    private static final String NHS_SPINE_ASID_SYSTEM = "https://fhir.nhs.uk/Id/nhsSpineASID";
    private static final String NHS_MHS_ID_SYSTEM = "https://fhir.nhs.uk/Id/nhsMHSId";
    private static final String TEST_ASID = "928940000057";
    private static final String TEST_MHS_ID = "mhs-id-123";
    private static final String TEST_ADDRESS = "https://gpc-provider.example.com/fhir";

    private final IParser fhirParser = FhirContext.forDstu3().newJsonParser();

    @Mock
    private SdsRequestBuilder sdsRequestBuilder;

    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestHeadersSpec deviceRequest;

    @SuppressWarnings("rawtypes")
    @Mock
    private WebClient.RequestHeadersSpec endpointRequest;

    @Mock
    private WebClient.ResponseSpec deviceResponseSpec;

    @Mock
    private WebClient.ResponseSpec endpointResponseSpec;

    private SdsClient sdsClient;

    @BeforeEach
    void setUp() {
        sdsClient = new SdsClient(fhirParser, sdsRequestBuilder);
        ReflectionTestUtils.setField(sdsClient, "supplierOdsCode", SUPPLIER_ODS_CODE);
    }

    @Test
    void When_DeviceBundleContainsSpineAsid_Expect_CallForGetAsidReturnsAsid() {
        when(sdsRequestBuilder.buildAsDeviceAsidRequest(FROM_ODS_CODE, SUPPLIER_ODS_CODE, INTERACTION_ID, CORRELATION_ID))
            .thenReturn(deviceRequest);
        stubDeviceResponse(buildDeviceBundle(TEST_ASID));

        StepVerifier.create(sdsClient.callForGetAsid(INTERACTION_ID, FROM_ODS_CODE, CORRELATION_ID))
            .assertNext(asid -> assertEquals(TEST_ASID, asid))
            .verifyComplete();
    }

    @Test
    void When_DeviceBundleHasNoEntries_Expect_CallForGetAsidErrors() {
        when(sdsRequestBuilder.buildAsDeviceAsidRequest(FROM_ODS_CODE, SUPPLIER_ODS_CODE, INTERACTION_ID, CORRELATION_ID))
            .thenReturn(deviceRequest);
        stubDeviceResponse(buildEmptyBundle());

        StepVerifier.create(sdsClient.callForGetAsid(INTERACTION_ID, FROM_ODS_CODE, CORRELATION_ID))
            .expectErrorSatisfies(e -> assertThat(e).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("SDS returned no result"))
            .verify();
    }

    @Test
    void When_DeviceMissingSpineAsidIdentifier_Expect_CallForGetAsidErrors() {
        when(sdsRequestBuilder.buildAsDeviceAsidRequest(FROM_ODS_CODE, SUPPLIER_ODS_CODE, INTERACTION_ID, CORRELATION_ID))
            .thenReturn(deviceRequest);
        stubDeviceResponse(buildDeviceBundleWithWrongIdentifierSystem());

        StepVerifier.create(sdsClient.callForGetAsid(INTERACTION_ID, FROM_ODS_CODE, CORRELATION_ID))
            .expectErrorSatisfies(e -> {
                assertThat(e).isInstanceOf(RuntimeException.class);
                assertThat(e.getMessage()).contains("Identifier of system");
            })
            .verify();
    }

    @Test
    void When_BothBundlesAreValid_Expect_CallForGetStructuredRecordReturnsSdsResponseData() {
        when(sdsRequestBuilder.buildGetStructuredRecordAsDeviceRequest(FROM_ODS_CODE, CORRELATION_ID))
            .thenReturn(deviceRequest);
        when(sdsRequestBuilder.buildGetStructuredRecordEndpointRequest(FROM_ODS_CODE, CORRELATION_ID))
            .thenReturn(endpointRequest);
        stubDeviceResponse(buildDeviceBundle(TEST_ASID));
        stubEndpointResponse(buildEndpointBundle(TEST_ADDRESS, TEST_MHS_ID));

        StepVerifier.create(sdsClient.callForGetStructuredRecord(FROM_ODS_CODE, CORRELATION_ID))
            .assertNext(data -> {
                assertEquals(TEST_ADDRESS, data.getAddress());
                assertEquals(TEST_ASID, data.getNhsSpineAsid());
                assertEquals(TEST_MHS_ID, data.getNhsMhsId());
            })
            .verifyComplete();
    }
  
    @Test
    void When_DeviceBundleHasNoEntries_Expect_CallForGetStructuredRecordErrors() {
        when(sdsRequestBuilder.buildGetStructuredRecordAsDeviceRequest(FROM_ODS_CODE, CORRELATION_ID))
                .thenReturn(deviceRequest);
        when(sdsRequestBuilder.buildGetStructuredRecordEndpointRequest(FROM_ODS_CODE, CORRELATION_ID))
                .thenReturn(endpointRequest);
        stubDeviceResponse(buildEmptyBundle());

        StepVerifier.create(sdsClient.callForGetStructuredRecord(FROM_ODS_CODE, CORRELATION_ID))
                .expectErrorSatisfies(e -> assertThat(e).isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("SDS returned no result"))
                .verify();
    }

    @Test
    void When_EndpointBundleHasNoEntries_Expect_CallForGetStructuredRecordErrors() {
        when(sdsRequestBuilder.buildGetStructuredRecordAsDeviceRequest(FROM_ODS_CODE, CORRELATION_ID))
                .thenReturn(deviceRequest);
        when(sdsRequestBuilder.buildGetStructuredRecordEndpointRequest(FROM_ODS_CODE, CORRELATION_ID))
                .thenReturn(endpointRequest);
        stubDeviceResponse(buildDeviceBundle(TEST_ASID));
        stubEndpointResponse(buildEmptyBundle());

        StepVerifier.create(sdsClient.callForGetStructuredRecord(FROM_ODS_CODE, CORRELATION_ID))
                .expectErrorSatisfies(e -> assertThat(e).isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("SDS returned no result"))
                .verify();
    }

    @Test
    void When_EndpointAddressIsBlank_Expect_CallForGetStructuredRecordErrors() {
        when(sdsRequestBuilder.buildGetStructuredRecordAsDeviceRequest(FROM_ODS_CODE, CORRELATION_ID))
                .thenReturn(deviceRequest);
        when(sdsRequestBuilder.buildGetStructuredRecordEndpointRequest(FROM_ODS_CODE, CORRELATION_ID))
                .thenReturn(endpointRequest);
        stubDeviceResponse(buildDeviceBundle(TEST_ASID));
        stubEndpointResponse(buildEndpointBundle("", TEST_MHS_ID));

        StepVerifier.create(sdsClient.callForGetStructuredRecord(FROM_ODS_CODE, CORRELATION_ID))
                .expectErrorSatisfies(e -> assertThat(e).isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("empty address"))
                .verify();
    }

    @Test
    void When_EndpointMissingNhsMhsIdIdentifier_Expect_CallForGetStructuredRecordErrors() {
        when(sdsRequestBuilder.buildGetStructuredRecordAsDeviceRequest(FROM_ODS_CODE, CORRELATION_ID))
                .thenReturn(deviceRequest);
        when(sdsRequestBuilder.buildGetStructuredRecordEndpointRequest(FROM_ODS_CODE, CORRELATION_ID))
                .thenReturn(endpointRequest);
        stubDeviceResponse(buildDeviceBundle(TEST_ASID));
        stubEndpointResponse(buildEndpointBundleWithoutMhsId(TEST_ADDRESS));

        StepVerifier.create(sdsClient.callForGetStructuredRecord(FROM_ODS_CODE, CORRELATION_ID))
                .expectErrorSatisfies(e -> assertThat(e).isInstanceOf(RuntimeException.class)
                        .hasMessageContaining(NHS_MHS_ID_SYSTEM))
                .verify();
    }

    @Test
    void When_DeviceBundleHasMultipleEntries_Expect_CallForGetStructuredRecordSucceeds() {
        when(sdsRequestBuilder.buildGetStructuredRecordAsDeviceRequest(FROM_ODS_CODE, CORRELATION_ID))
                .thenReturn(deviceRequest);
        when(sdsRequestBuilder.buildGetStructuredRecordEndpointRequest(FROM_ODS_CODE, CORRELATION_ID))
                .thenReturn(endpointRequest);
        stubDeviceResponse(buildDeviceBundleWithMultipleEntries(TEST_ASID, "other-asid"));
        stubEndpointResponse(buildEndpointBundle(TEST_ADDRESS, TEST_MHS_ID));

        StepVerifier.create(sdsClient.callForGetStructuredRecord(FROM_ODS_CODE, CORRELATION_ID))
                .assertNext(data -> assertThat(data.getNhsSpineAsid()).isEqualTo(TEST_ASID))
                .verifyComplete();
    }

    @Test
    void When_CallingForMigrateStructuredRecord_Expect_CorrectBuilderMethodsInvoked() {
        when(sdsRequestBuilder.buildMigrateStructuredRecordAsDeviceRequest(FROM_ODS_CODE, CORRELATION_ID))
                .thenReturn(deviceRequest);
        when(sdsRequestBuilder.buildMigrateStructuredRecordEndpointRequest(FROM_ODS_CODE, CORRELATION_ID))
                .thenReturn(endpointRequest);
        stubDeviceResponse(buildDeviceBundle(TEST_ASID));
        stubEndpointResponse(buildEndpointBundle(TEST_ADDRESS, TEST_MHS_ID));

        sdsClient.callForMigrateStructuredRecord(FROM_ODS_CODE, CORRELATION_ID).block();

        verify(sdsRequestBuilder).buildMigrateStructuredRecordAsDeviceRequest(FROM_ODS_CODE, CORRELATION_ID);
        verify(sdsRequestBuilder).buildMigrateStructuredRecordEndpointRequest(FROM_ODS_CODE, CORRELATION_ID);
    }

    @Test
    void When_CallingForPatientSearchAccessDocument_Expect_CorrectBuilderMethodsInvoked() {
        when(sdsRequestBuilder.buildPatientSearchAccessDocumentAsDeviceRequest(FROM_ODS_CODE, CORRELATION_ID))
                .thenReturn(deviceRequest);
        when(sdsRequestBuilder.buildPatientSearchAccessDocumentEndpointRequest(FROM_ODS_CODE, CORRELATION_ID))
                .thenReturn(endpointRequest);
        stubDeviceResponse(buildDeviceBundle(TEST_ASID));
        stubEndpointResponse(buildEndpointBundle(TEST_ADDRESS, TEST_MHS_ID));

        sdsClient.callForPatientSearchAccessDocument(FROM_ODS_CODE, CORRELATION_ID).block();

        verify(sdsRequestBuilder).buildPatientSearchAccessDocumentAsDeviceRequest(FROM_ODS_CODE, CORRELATION_ID);
        verify(sdsRequestBuilder).buildPatientSearchAccessDocumentEndpointRequest(FROM_ODS_CODE, CORRELATION_ID);
    }

    @Test
    void When_CallingForSearchForDocumentRecord_Expect_CorrectBuilderMethodsInvoked() {
        when(sdsRequestBuilder.buildSearchForDocumentAsDeviceRequest(FROM_ODS_CODE, CORRELATION_ID))
                .thenReturn(deviceRequest);
        when(sdsRequestBuilder.buildSearchForDocumentEndpointRequest(FROM_ODS_CODE, CORRELATION_ID))
                .thenReturn(endpointRequest);
        stubDeviceResponse(buildDeviceBundle(TEST_ASID));
        stubEndpointResponse(buildEndpointBundle(TEST_ADDRESS, TEST_MHS_ID));

        sdsClient.callForSearchForDocumentRecord(FROM_ODS_CODE, CORRELATION_ID).block();

        verify(sdsRequestBuilder).buildSearchForDocumentAsDeviceRequest(FROM_ODS_CODE, CORRELATION_ID);
        verify(sdsRequestBuilder).buildSearchForDocumentEndpointRequest(FROM_ODS_CODE, CORRELATION_ID);
    }

    @Test
    void When_CallingForRetrieveDocumentRecord_Expect_CorrectBuilderMethodsInvoked() {
        when(sdsRequestBuilder.buildRetrieveDocumentAsDeviceRequest(FROM_ODS_CODE, CORRELATION_ID))
                .thenReturn(deviceRequest);
        when(sdsRequestBuilder.buildRetrieveDocumentEndpointRequest(FROM_ODS_CODE, CORRELATION_ID))
                .thenReturn(endpointRequest);
        stubDeviceResponse(buildDeviceBundle(TEST_ASID));
        stubEndpointResponse(buildEndpointBundle(TEST_ADDRESS, TEST_MHS_ID));

        sdsClient.callForRetrieveDocumentRecord(FROM_ODS_CODE, CORRELATION_ID).block();

        verify(sdsRequestBuilder).buildRetrieveDocumentAsDeviceRequest(FROM_ODS_CODE, CORRELATION_ID);
        verify(sdsRequestBuilder).buildRetrieveDocumentEndpointRequest(FROM_ODS_CODE, CORRELATION_ID);
    }

    @Test
    void When_CallingForMigrateDocumentRecord_Expect_CorrectBuilderMethodsInvoked() {
        when(sdsRequestBuilder.buildMigrateDocumentAsDeviceRequest(FROM_ODS_CODE, CORRELATION_ID))
                .thenReturn(deviceRequest);
        when(sdsRequestBuilder.buildMigrateDocumentEndpointRequest(FROM_ODS_CODE, CORRELATION_ID))
                .thenReturn(endpointRequest);
        stubDeviceResponse(buildDeviceBundle(TEST_ASID));
        stubEndpointResponse(buildEndpointBundle(TEST_ADDRESS, TEST_MHS_ID));

        sdsClient.callForMigrateDocumentRecord(FROM_ODS_CODE, CORRELATION_ID).block();

        verify(sdsRequestBuilder).buildMigrateDocumentAsDeviceRequest(FROM_ODS_CODE, CORRELATION_ID);
        verify(sdsRequestBuilder).buildMigrateDocumentEndpointRequest(FROM_ODS_CODE, CORRELATION_ID);
    }

    @SuppressWarnings("unchecked")
    private void stubDeviceResponse(String json) {
        when(deviceRequest.retrieve()).thenReturn(deviceResponseSpec);
        when(deviceResponseSpec.bodyToMono(eq(String.class))).thenReturn(Mono.just(json));
    }

    @SuppressWarnings("unchecked")
    private void stubEndpointResponse(String json) {
        when(endpointRequest.retrieve()).thenReturn(endpointResponseSpec);
        when(endpointResponseSpec.bodyToMono(eq(String.class))).thenReturn(Mono.just(json));
    }

    private String buildDeviceBundle(String asid) {
        var bundle = new Bundle();
        var device = new Device();
        device.addIdentifier().setSystem(NHS_SPINE_ASID_SYSTEM).setValue(asid);
        bundle.addEntry().setResource(device);
        return fhirParser.encodeResourceToString(bundle);
    }

    private String buildDeviceBundleWithMultipleEntries(String firstAsid, String secondAsid) {
        var bundle = new Bundle();
        var device1 = new Device();
        device1.addIdentifier().setSystem(NHS_SPINE_ASID_SYSTEM).setValue(firstAsid);
        var device2 = new Device();
        device2.addIdentifier().setSystem(NHS_SPINE_ASID_SYSTEM).setValue(secondAsid);
        bundle.addEntry().setResource(device1);
        bundle.addEntry().setResource(device2);
        return fhirParser.encodeResourceToString(bundle);
    }

    private String buildDeviceBundleWithWrongIdentifierSystem() {
        var bundle = new Bundle();
        var device = new Device();
        device.addIdentifier().setSystem("https://fhir.nhs.uk/Id/WRONG_SYSTEM").setValue("some-value");
        bundle.addEntry().setResource(device);
        return fhirParser.encodeResourceToString(bundle);
    }

    private String buildDeviceBundleWithoutIdentifier() {
        var bundle = new Bundle();
        bundle.addEntry().setResource(new Device());
        return fhirParser.encodeResourceToString(bundle);
    }

    private String buildEndpointBundle(String address, String nhsMhsId) {
        var bundle = new Bundle();
        var endpoint = new Endpoint();
        endpoint.setAddress(address);
        endpoint.addIdentifier().setSystem(NHS_MHS_ID_SYSTEM).setValue(nhsMhsId);
        bundle.addEntry().setResource(endpoint);
        return fhirParser.encodeResourceToString(bundle);
    }

    private String buildEndpointBundleWithoutMhsId(String address) {
        var bundle = new Bundle();
        var endpoint = new Endpoint();
        endpoint.setAddress(address);
        bundle.addEntry().setResource(endpoint);
        return fhirParser.encodeResourceToString(bundle);
    }

    private String buildEmptyBundle() {
        return fhirParser.encodeResourceToString(new Bundle());
    }
}
