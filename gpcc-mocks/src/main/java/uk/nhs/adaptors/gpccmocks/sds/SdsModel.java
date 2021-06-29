package uk.nhs.adaptors.gpccmocks.sds;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString
public class SdsModel {
    private String baseUrl;
    private String odsCode;
    private String interactionId;
    private String fhirBase;
}
