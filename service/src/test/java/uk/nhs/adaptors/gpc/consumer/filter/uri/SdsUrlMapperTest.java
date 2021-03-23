package uk.nhs.adaptors.gpc.consumer.filter.uri;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.server.RequestPath;

import uk.nhs.adaptors.gpc.consumer.filters.uri.SdsUrlMapper;
import uk.nhs.adaptors.gpc.consumer.gpc.GpcConfiguration;

@ExtendWith(MockitoExtension.class)
public class SdsUrlMapperTest {

    private static final String REQUEST_URL = "http://consumer.adaptor.com/GPABC123/STU3/1/gpconnect/documents/Patient/2/"
        + "DocumentReference?_include=DocumentReference%3Asubject%3APatient&_include=DocumentReference";
    private static final String SDS_LOOKUP = "https://supplierabc.internal.nhs.net/ABC/5/gpcdocuments";
    private static final String EXPECTED_URL = "https://supplierabc.internal.nhs.net/ABC/5/gpcdocuments/Patient/2"
        + "/DocumentReference?_include=DocumentReference%3Asubject%3APatient&_include=DocumentReference";
    private static final String SLASH = "/";

    @Mock
    private GpcConfiguration gpcConfiguration;

    private SdsUrlMapper sdsUrlMapper;

    @BeforeEach
    public void setUp() {
        when(gpcConfiguration.getStructuredFhirBasePathRegex()).thenReturn("(.*?)\\/STU3/1/gpconnect/(.*?)\\/");

        sdsUrlMapper = new SdsUrlMapper(gpcConfiguration);
    }

    @Test
    public void When_JoiningSdsLookUpUrlWithRequestUrl_Expect_StructuredFhirBaseRemoved() {
        String sdsUrlResult = sdsUrlMapper.map(SDS_LOOKUP, RequestPath.parse(REQUEST_URL, null));

        assertThat(sdsUrlResult).isEqualTo(EXPECTED_URL);
    }

    @Test
    public void When_JoiningSdsLookUpUrlWithSlashAtTheEndWithRequestUrl_Expect_SlashNotDuplicated() {
        String sdsUrlResult = sdsUrlMapper.map(SDS_LOOKUP + SLASH, RequestPath.parse(REQUEST_URL, null));

        assertThat(sdsUrlResult).isEqualTo(EXPECTED_URL);
    }
}
