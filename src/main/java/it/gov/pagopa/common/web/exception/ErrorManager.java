package it.gov.pagopa.common.web.exception;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Optional;

@RestControllerAdvice
@Slf4j
public class ErrorManager {
  private final ErrorDTO defaultErrorDTO;

  public ErrorManager(@Nullable ErrorDTO defaultErrorDTO) {
    this.defaultErrorDTO = Optional.ofNullable(defaultErrorDTO)
            .orElse(new ErrorDTO("Error", "Something gone wrong"));
  }

  @ExceptionHandler(RuntimeException.class)
  protected ResponseEntity<ErrorDTO> handleException(RuntimeException error, HttpServletRequest request) {

    logClientException(error, request);

    HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;

    return ResponseEntity.status(httpStatus)
            .contentType(MediaType.APPLICATION_JSON)
            .body(defaultErrorDTO);

  }

  public static void logClientException(RuntimeException error, HttpServletRequest request) {
    Throwable unwrappedException = error.getCause() instanceof ServiceException
            ? error.getCause()
            : error;

    String clientExceptionMessage = "";


    log.info("A {} occurred handling request {}{} at {}",
            unwrappedException.getClass().getSimpleName() ,
            getRequestDetails(request),
            clientExceptionMessage,
            unwrappedException.getStackTrace().length > 0 ? unwrappedException.getStackTrace()[0] : "UNKNOWN");

  }

  public static String getRequestDetails(HttpServletRequest request) {
    return "%s %s".formatted(request.getMethod(), request.getRequestURI());
  }
}
