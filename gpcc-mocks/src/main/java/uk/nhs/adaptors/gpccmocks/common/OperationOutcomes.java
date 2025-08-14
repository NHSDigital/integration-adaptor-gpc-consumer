package uk.nhs.adaptors.gpccmocks.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class OperationOutcomes {

    public static final String OPERATION_OUTCOME = "operationOutcome";

    public static ResponseEntity<String> badRequest(String message) {
        var model = OperationOutcomeModel.builder()
            .fhirCode("structure")
            .spineCode("BAD_REQUEST")
            .message(message)
            .build();
        var body = TemplateUtils.fillTemplate(OPERATION_OUTCOME, model);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    public static ResponseEntity<String> invalidNhsNumber(String message) {
        var model = OperationOutcomeModel.builder()
            .fhirCode("structure")
            .spineCode("INVALID_NHS_NUMBER")
            .message(message)
            .build();
        var body = TemplateUtils.fillTemplate(OPERATION_OUTCOME, model);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    public static ResponseEntity<String> patientNotFound(String message) {
        var model = OperationOutcomeModel.builder()
            .fhirCode("not-found")
            .spineCode("PATIENT_NOT_FOUND")
            .message(message)
            .build();
        var body = TemplateUtils.fillTemplate(OPERATION_OUTCOME, model);
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    public static ResponseEntity<String> referenceNotFound(String message) {
        var model = OperationOutcomeModel.builder()
            .fhirCode("not-found")
            .spineCode("REFERENCE_NOT_FOUND")
            .message(message)
            .build();
        var body = TemplateUtils.fillTemplate(OPERATION_OUTCOME, model);
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }
}
