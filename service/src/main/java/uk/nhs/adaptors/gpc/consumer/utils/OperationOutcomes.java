package uk.nhs.adaptors.gpc.consumer.utils;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class OperationOutcomes {

    public static final String OPERATION_OUTCOME = "operationOutcome";

    public static ResponseEntity<String> buildErrorResponse(HttpStatus status, String spineCode, String fhirCode, String message) {
        var model = OperationOutcomeModel.builder()
            .fhirCode(fhirCode)
            .spineCode(spineCode)
            .message(message)
            .build();
        var body = TemplateUtils.fillTemplate(OPERATION_OUTCOME, model);
        return new ResponseEntity<>(body, status);
    }

}
