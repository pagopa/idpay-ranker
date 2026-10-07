package it.gov.pagopa.ranker.exception;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ControllerExceptionHandler {

    private static final String INVALID_SIZE_CODE = "INVALID_SIZE";

    @ExceptionHandler(ManualDequeueSizeNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorDTO handleManualDequeueSizeNotValidException(ManualDequeueSizeNotValidException ex) {
        return new ErrorDTO(INVALID_SIZE_CODE, ex.getMessage());
    }

    // TODO: Should we handle MethodArgumentTypeMismatchException as well?
    // -> size is an int, caller might pass a string
}
