package uk.nhs.adaptors.gpccmocks.gpc;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GpcModel {
    private String baseUrl;
    private String odsCode;
    private String nhsNumber;
    private boolean hasDocuments;
    private String patientId;
    private String documentId;
}
