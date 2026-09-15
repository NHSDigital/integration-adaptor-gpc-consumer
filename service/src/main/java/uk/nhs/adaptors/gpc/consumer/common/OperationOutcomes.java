package uk.nhs.adaptors.gpc.consumer.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class OperationOutcomes {
    public static final String OPERATION_OUTCOME = "operationOutcome";

    public static ResponseEntity<String> internalServerError(String message) {
        var model = OperationOutcomeModel.builder()
                .fhirCode("exception")
                .spineCode("INTERNAL_SERVER_ERROR")
                .message(message)
                .build();
        var body = TemplateUtils.fillTemplate(OPERATION_OUTCOME, model);
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}