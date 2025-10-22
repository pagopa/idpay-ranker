package it.gov.pagopa.ranker.exception;

public class UnableToAddSeqException extends RuntimeException {
    public UnableToAddSeqException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnableToAddSeqException(String message) {
        super(message);
    }
}
