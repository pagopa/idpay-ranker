package it.gov.pagopa.ranker.exception;

public class UnableToRemoveSeqException extends RuntimeException {
    public UnableToRemoveSeqException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnableToRemoveSeqException(String message) {
        super(message);
    }
}
