package uk.nhs.adaptors.gpc.consumer.gpc;

import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import uk.nhs.adaptors.gpc.consumer.filters.SdsFilter;
import uk.nhs.adaptors.gpc.consumer.sds.SdsClient;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SdsFilterTest {

    @Mock
    private SdsClient sdsClient;
    private SdsFilter sdsFilter;
    private GatewayFilterChain filterChain;
    private ArgumentCaptor<ServerWebExchange> captor;

    private static final String MIGRATE_STRUCTURED_INTERACTION
                                                    = "urn:nhs:names:services:gpconnect:fhir:operation:gpc.migratestructuredrecord-1";

    @BeforeEach
    @SneakyThrows
    public void before() {
        sdsFilter = new SdsFilter(sdsClient);
        sdsFilter.initializeSdsRequestFunctions();

        filterChain = Mockito.mock(GatewayFilterChain.class);
        captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        when(filterChain.filter(captor.capture())).thenReturn(Mono.empty());
    }

    @Test
    @SneakyThrows
    public void When_NoSspHeaders_Expect_FetchValuesFromSds() {

        String odsCode = "A12345";
        String correlationId = "98765";
        String gpConnectServerAsid = "928940000057";
        String gpConnectConsumerAsid = "928942012545";

        MockServerHttpRequest request
                        = MockServerHttpRequest.get("/A12345/STU3/1/gpconnect/fhir/Patient/$gpc.migratestructuredrecord")
                        .header("Ssp-TraceID", correlationId)
                        .header("Ssp-InteractionID",
                                "urn:nhs:names:services:gpconnect:fhir:operation:gpc.migratestructuredrecord-1").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(sdsClient.callForGetAsid(MIGRATE_STRUCTURED_INTERACTION, odsCode, correlationId)).thenReturn(Mono.just(gpConnectConsumerAsid));
        when(sdsClient.callForMigrateStructuredRecord(odsCode, correlationId))
                                    .thenReturn(Mono.just(SdsClient.SdsResponseData.builder().nhsSpineAsid(gpConnectServerAsid).build()));

        sdsFilter.filter(exchange, filterChain).block();

        var resultExchange = captor.getValue();
        assertEquals(gpConnectConsumerAsid, resultExchange.getRequest().getHeaders().get("ssp-From").get(0));
        assertEquals(gpConnectServerAsid, resultExchange.getRequest().getHeaders().get("ssp-To").get(0));
    }

    @Test
    @SneakyThrows
    public void When_SspValuesHeadersPresent_Expect_FetchSspValuesFromRequestHeaders() {

        String odsCode = "A12345";
        String correlationId = "98765";
        String gpConnectServerAsid = "928940000001";
        String gpConnectConsumerAsid = "928940000005";

        MockServerHttpRequest request
            = MockServerHttpRequest.get("/A12345/STU3/1/gpconnect/fhir/Patient/$gpc.migratestructuredrecord")
            .header("Ssp-TraceID", correlationId)
            .header("Ssp-InteractionID",
                    "urn:nhs:names:services:gpconnect:fhir:operation:gpc.migratestructuredrecord-1")
            .header("Ssp-To", gpConnectServerAsid)
            .header("Ssp-From", gpConnectConsumerAsid)
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(sdsClient.callForGetAsid(MIGRATE_STRUCTURED_INTERACTION, odsCode, correlationId)).thenReturn(Mono.just("928942012545"));
        when(sdsClient.callForMigrateStructuredRecord(odsCode, correlationId))
            .thenReturn(Mono.just(SdsClient.SdsResponseData.builder().nhsSpineAsid("928940000057").build()));

        sdsFilter.filter(exchange, filterChain).block();

        var resultExchange = captor.getValue();
        assertEquals(gpConnectConsumerAsid, resultExchange.getRequest().getHeaders().get("ssp-From").get(0));
        assertEquals(gpConnectServerAsid, resultExchange.getRequest().getHeaders().get("ssp-To").get(0));
    }

}
