package uk.nhs.adaptors.gpc.consumer.sds.builder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

import io.netty.handler.ssl.SslContext;
import reactor.core.publisher.Mono;
import uk.nhs.adaptors.gpc.consumer.gpc.exception.GpConnectException;
import uk.nhs.adaptors.gpc.consumer.sds.configuration.SdsConfiguration;
import uk.nhs.adaptors.gpc.consumer.web.RequestBuilderService;
import uk.nhs.adaptors.gpc.consumer.web.WebClientFilterService;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SdsRequestBuilderTest {

    private static final String ODS_CODE = "A12345";
    private static final String SUPPLIER_ODS_CODE = "SUPPLIER01";
    private static final String CORRELATION_ID = "corr-id-001";
    private static final String INTERACTION_ID = "urn:nhs:names:services:gpconnect:fhir:operation:gpc.getstructuredrecord-1";
    private static final String SDS_URL = "https://sds.example.com";
    private static final String API_KEY = "test-api-key";

    @Mock
    private SdsConfiguration sdsConfiguration;

    @Mock
    private RequestBuilderService requestBuilderService;

    @Mock
    private WebClientFilterService webClientFilterService;

    @Mock
    private SslContext sslContext;

    private SdsRequestBuilder sdsRequestBuilder;

    @BeforeEach
    void setUp() {
        sdsRequestBuilder = new SdsRequestBuilder(sdsConfiguration, requestBuilderService, webClientFilterService);
        when(requestBuilderService.buildStandardSslContext()).thenReturn(sslContext);
        when(requestBuilderService.buildExchangeStrategies()).thenReturn(ExchangeStrategies.withDefaults());
        when(sdsConfiguration.getUrl()).thenReturn(SDS_URL);
        when(sdsConfiguration.getApiKey()).thenReturn(API_KEY);
        when(webClientFilterService.logRequest())
                .thenReturn((request, next) -> next.exchange(request));
        when(webClientFilterService.logResponse())
                .thenReturn(ExchangeFilterFunction.ofResponseProcessor(Mono::just));
        when(webClientFilterService.errorHandlingFilter(any(), any()))
                .thenReturn(ExchangeFilterFunction.ofResponseProcessor(Mono::just));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void When_SupplierOdsCodeIsBlank_Expect_GpConnectException(String blankSupplierOdsCode) {
        assertThatThrownBy(() -> sdsRequestBuilder.buildAsDeviceAsidRequest(ODS_CODE, blankSupplierOdsCode, INTERACTION_ID, CORRELATION_ID))
            .isInstanceOf(GpConnectException.class)
            .hasMessageContaining("Supplier ODS code variable must be defined");
    }

    @Test
    void When_SupplierOdsCodeIsPresent_Expect_BuildAsDeviceAsidRequestReturnsRequest() {

        var result = sdsRequestBuilder.buildAsDeviceAsidRequest(ODS_CODE, SUPPLIER_ODS_CODE, INTERACTION_ID, CORRELATION_ID);

        assertNotNull(result);
    }

    @Test
    void When_Called_Expect_BuildGetStructuredRecordAsDeviceRequestReturnsRequest() {
        assertNotNull(sdsRequestBuilder.buildGetStructuredRecordAsDeviceRequest(ODS_CODE, CORRELATION_ID));
    }

    @Test
    void When_Called_Expect_BuildGetStructuredRecordEndpointRequestReturnsRequest() {
        assertNotNull(sdsRequestBuilder.buildGetStructuredRecordEndpointRequest(ODS_CODE, CORRELATION_ID));
    }

    @Test
    void When_Called_Expect_BuildMigrateStructuredRecordAsDeviceRequestReturnsRequest() {
        assertNotNull(sdsRequestBuilder.buildMigrateStructuredRecordAsDeviceRequest(ODS_CODE, CORRELATION_ID));
    }

    @Test
    void When_Called_Expect_BuildMigrateStructuredRecordEndpointRequestReturnsRequest() {
        assertNotNull(sdsRequestBuilder.buildMigrateStructuredRecordEndpointRequest(ODS_CODE, CORRELATION_ID));
    }

    @Test
    void When_Called_Expect_BuildPatientSearchAccessDocumentAsDeviceRequestReturnsRequest() {
        assertNotNull(sdsRequestBuilder.buildPatientSearchAccessDocumentAsDeviceRequest(ODS_CODE, CORRELATION_ID));
    }
}
