package uk.nhs.adaptors.gpc.consumer.filter.uri;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import uk.nhs.adaptors.gpc.consumer.filters.uri.SspUriBuilder;

@ExtendWith(MockitoExtension.class)
public class SspUriBuilderTest {

    private static final String SSP_DOMAIN = "proxy.int.spine2.ncrs.nhs.uk";
    private static final String SDS_LOOKUP_ADDRESS = "https://supplierabc.internal.nhs.net/ABC/5/gpcdocuments";
    private static final String FHIR_RESOURCE_REGEX = "(.*?)\\/STU3/1/gpconnect/(.*?)\\/";
    private static final String GPC_ADDRESS = "https://messagingportal.opentest.hscic.gov.uk:19192";
    private static final String INITIAL_REQUEST_PATH = "/GPABC123/STU3/1/gpconnect/documents/Patient/2/"
        + "DocumentReference?_include=DocumentReference";
    private static final MultiValueMap<String, String> QUERY_PARAMS = new LinkedMultiValueMap<>();

    @Mock
    private ServerHttpRequest request;

    @BeforeEach
    public void setUp() {
        when(request.getQueryParams()).thenReturn(QUERY_PARAMS);
    }

    @Test
    public void When_BuildingSSPUriWithUriReturnedFromSDS_Expect_CorrectUriBuilt() {
        Optional<URI> sspUriBuilder = new SspUriBuilder()
            .sspDomain(SSP_DOMAIN)
            .initialPath(INITIAL_REQUEST_PATH)
            .address(SDS_LOOKUP_ADDRESS)
            .structuredFhirBaseRegex(FHIR_RESOURCE_REGEX)
            .buildSDS(request);

        assertThat(sspUriBuilder.isPresent()).isTrue();
        assertThat(sspUriBuilder.get().toString())
            .isEqualTo("https://proxy.int.spine2.ncrs.nhs.uk/https:/supplierabc.internal.nhs.net/ABC/5/gpcdocuments/Patient/2/DocumentReference?_include=DocumentReference");
    }

    @Test
    public void When_BuildingSSPUriWithDirectGPCUrlFromConfiguration_Expect_CorrectUriBuilt() {
        Optional<URI> sspUriBuilder = new SspUriBuilder()
            .sspDomain(SSP_DOMAIN)
            .initialPath(INITIAL_REQUEST_PATH)
            .address(GPC_ADDRESS)
            .buildDirectGPC(request);

        assertThat(sspUriBuilder.isPresent()).isTrue();
        assertThat(sspUriBuilder.get().toString())
            .isEqualTo("https://proxy.int.spine2.ncrs.nhs.uk/https:/messagingportal.opentest.hscic.gov.uk:19192/GPABC123/STU3/1/gpconnect/documents/Patient/2/DocumentReference?_include=DocumentReference");
    }
}