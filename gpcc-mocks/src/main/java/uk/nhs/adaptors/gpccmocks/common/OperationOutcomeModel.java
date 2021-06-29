package uk.nhs.adaptors.gpccmocks.common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OperationOutcomeModel {
    private String fhirCode;
    private String spineCode;
    private String message;
}
