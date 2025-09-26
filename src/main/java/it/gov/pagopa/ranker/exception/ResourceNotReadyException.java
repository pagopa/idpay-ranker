package it.gov.pagopa.ranker.exception;

import it.gov.pagopa.ranker.constants.ErrorMessages;

public class ResourceNotReadyException extends RuntimeException {

    public ResourceNotReadyException() {
        super(ErrorMessages.RESOURCE_NOT_READY);
    }
}
