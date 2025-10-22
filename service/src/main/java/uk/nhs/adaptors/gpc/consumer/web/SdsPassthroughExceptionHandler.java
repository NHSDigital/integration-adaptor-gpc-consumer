package uk.nhs.adaptors.gpc.consumer.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uk.nhs.adaptors.gpc.consumer.sds.SdsPassthroughException;

@RestControllerAdvice
public class SdsPassthroughExceptionHandler {

    @ExceptionHandler(SdsPassthroughException.class)
    public ResponseEntity<String> handleSds(SdsPassthroughException ex) {

        return ex.toResponseEntity();
    }
}
