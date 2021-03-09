package uk.nhs.adaptors.gpc.consumer.sds.exception;

public class SdsException extends RuntimeException {
    public SdsException(String message) {
        super(message);
    }

    public SdsException(String message, Throwable cause) {
        super(message, cause);
    }
}
