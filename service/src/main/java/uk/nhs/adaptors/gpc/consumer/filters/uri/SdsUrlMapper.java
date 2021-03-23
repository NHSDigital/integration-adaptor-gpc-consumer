package uk.nhs.adaptors.gpc.consumer.filters.uri;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.RequestPath;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import uk.nhs.adaptors.gpc.consumer.gpc.GpcConfiguration;

@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SdsUrlMapper {
    public static final String SLASH = "/";
    private final GpcConfiguration gpcConfiguration;

    public String map(String sdsLookUpAddress, RequestPath requestPath) {
        String fhirRequest = requestPath.value().replaceFirst(gpcConfiguration.getStructuredFhirBasePathRegex(), StringUtils.EMPTY);
        String preparedSdsLookUpAddress = StringUtils.removeEnd(sdsLookUpAddress, SLASH);

        return preparedSdsLookUpAddress + SLASH + fhirRequest;
    }
}
