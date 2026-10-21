package it.gov.pagopa.ranker.constants;

public final class ErrorMessages {

    private ErrorMessages() {
        throw new IllegalStateException("Utility class");
    }

    public static final String RESOURCE_NOT_READY = "Counters or initiative  not ready for the requested initiative";
    public static final String INVALID_SIZE_NOT_POSITIVE = "size must be a positive number";
}
