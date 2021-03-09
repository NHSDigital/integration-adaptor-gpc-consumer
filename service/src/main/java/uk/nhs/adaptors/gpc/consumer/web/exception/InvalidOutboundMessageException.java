package uk.nhs.adaptors.gpc.consumer.web.exception;

public class InvalidOutboundMessageException extends RuntimeException {
    public InvalidOutboundMessageException(String message) {
        super(message);
    }
}
