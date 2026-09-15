package uk.nhs.adaptors.gpc.consumer.sds.builder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

import io.netty.handler.ssl.SslContext;
import reactor.core.publisher.Mono;
import uk.nhs.adaptors.gpc.consumer.sds.configuration.SdsConfiguration;
import uk.nhs.adaptors.gpc.consumer.web.RequestBuilderService;
import uk.nhs.adaptors.gpc.consumer.web.WebClientFilterService;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static uk.nhs.adaptors.gpc.consumer.TestConstants.GET_STRUCTURED_INTERACTION;
import static uk.nhs.adaptors.gpc.consumer.TestConstants.MIGRATE_DOCUMENT_INTERACTION;
import static uk.nhs.adaptors.gpc.consumer.TestConstants.MIGRATE_STRUCTURED_INTERACTION;
import static uk.nhs.adaptors.gpc.consumer.TestConstants.PATIENT_SEARCH_ACCESS_DOCUMENT_INTERACTION;
import static uk.nhs.adaptors.gpc.consumer.TestConstants.RETRIEVE_DOCUMENT_INTERACTION;
import static uk.nhs.adaptors.gpc.consumer.TestConstants.SEARCH_FOR_DOCUMENT_INTERACTION;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SdsRequestBuilderTest {

    private static final String ODS_CODE = "A12345";
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

    @Test
    void When_SupplierOdsCodeIsPresent_Expect_BuildAsDeviceAsidRequestReturnsRequest() {

        var result = sdsRequestBuilder.buildAsDeviceAsidRequest(ODS_CODE, INTERACTION_ID, CORRELATION_ID);

        assertNotNull(result);
    }

    public static String[] getInteractionUrns() {
        return new String[] {
            GET_STRUCTURED_INTERACTION,
            PATIENT_SEARCH_ACCESS_DOCUMENT_INTERACTION,
            SEARCH_FOR_DOCUMENT_INTERACTION,
            RETRIEVE_DOCUMENT_INTERACTION,
            MIGRATE_DOCUMENT_INTERACTION,
            MIGRATE_STRUCTURED_INTERACTION
        };
    }

    @ParameterizedTest
    @MethodSource("getInteractionUrns")
    void When_Called_Expect_BuildGetStructuredRecordAsDeviceRequestReturnsRequest(String interactionUrn) {
        assertNotNull(sdsRequestBuilder.buildDeviceRequest(ODS_CODE, CORRELATION_ID, interactionUrn));
    }

    @ParameterizedTest
    @MethodSource("getInteractionUrns")
    void When_Called_Expect_BuildGetStructuredRecordEndpointRequestReturnsRequest(String interactionUrn) {
        assertNotNull(sdsRequestBuilder.buildEndpointRequest(ODS_CODE, CORRELATION_ID, interactionUrn));
    }
}
