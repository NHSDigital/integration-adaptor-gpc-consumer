package uk.nhs.adaptors.gpc.consumer.sds;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;

/**
 * Signals that we should forward the SDS response (status + headers + body) unchanged to the caller.
 */
@Getter
@Slf4j
public class SdsPassthroughException extends RuntimeException {
    private final HttpStatus status;
    private final HttpHeaders headers;
    private final String body;

    public SdsPassthroughException(HttpStatus status, HttpHeaders headers, String body) {
        super("SDS passthrough " + status);
        this.status = status;
        this.headers = HttpHeaders.readOnlyHttpHeaders(headers);
        this.body = body;
    }

    public ResponseEntity<String> toResponseEntity() {
        HttpHeaders out = new HttpHeaders();
        out.putAll(headers);
        return new ResponseEntity<>(body, out, status);
    }
}
