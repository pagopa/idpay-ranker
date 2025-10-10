package it.gov.pagopa.ranker.exception;

public class BudgetExhaustedException extends RuntimeException {
    public BudgetExhaustedException(String message, Throwable cause) {
        super(message, cause);
    }

    public BudgetExhaustedException(String message) {
        super(message);
    }
}
