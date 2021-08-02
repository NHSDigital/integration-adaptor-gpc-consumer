package uk.nhs.adaptors.gpccmocks.gpc;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString
public class GpcModel {
    private String baseUrl;
    private String odsCode;
    private String nhsNumber;
    private boolean hasDocuments;
    private boolean binary1;
    private boolean binary2;
    private boolean binary3;
    private String patientId;
    private String documentId;
}
