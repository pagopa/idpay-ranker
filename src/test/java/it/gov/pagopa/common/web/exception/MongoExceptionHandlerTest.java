package it.gov.pagopa.common.web.exception;

import it.gov.pagopa.common.mongo.retry.MongoRequestRateTooLargeRetryer;
import it.gov.pagopa.common.mongo.retry.exception.MongoRequestRateTooLargeRetryExpiredException;
import it.gov.pagopa.common.web.dto.ErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MongoExceptionHandlerTest {
    private MongoExceptionHandler handler;
    private ErrorDTO customErrorDTO;

    @BeforeEach
    void setUp() {
        customErrorDTO = new ErrorDTO("TOO_MANY_REQUESTS", "Too Many Requests");
        handler = new MongoExceptionHandler(customErrorDTO);
    }

    @Test
    void testHandleDataAccessException_withRetryAfter() {
        DataAccessException ex = mock(DataAccessException.class);
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/test");

        try (MockedStatic<MongoRequestRateTooLargeRetryer> utilities = Mockito.mockStatic(MongoRequestRateTooLargeRetryer.class)) {
            utilities.when(() -> MongoRequestRateTooLargeRetryer.getRetryAfterMs(ex)).thenReturn(3500L);

            ResponseEntity<ErrorDTO> response = handler.handleDataAccessException(ex, request);

            assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
            assertEquals("TOO_MANY_REQUESTS", response.getBody().getCode());
            assertEquals("Too Many Requests", response.getBody().getMessage());
            assertEquals("4", response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)); // 3500 ms -> 4 s
            assertEquals("3500", response.getHeaders().getFirst("Retry-After-Ms"));
        }
    }

    @Test
    void testHandleDataAccessException_noRetryAfter() {
        DataAccessException ex = mock(DataAccessException.class);
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/example");

        try (MockedStatic<MongoRequestRateTooLargeRetryer> utilities = Mockito.mockStatic(MongoRequestRateTooLargeRetryer.class)) {
            utilities.when(() -> MongoRequestRateTooLargeRetryer.getRetryAfterMs(ex)).thenReturn(null);

            ResponseEntity<ErrorDTO> response = handler.handleDataAccessException(ex, request);

            assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
            assertEquals("TOO_MANY_REQUESTS", response.getBody().getCode());
            assertEquals("Too Many Requests", response.getBody().getMessage());
            assertNull(response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER));
            assertNull(response.getHeaders().getFirst("Retry-After-Ms"));
        }
    }

    @Test
    void testHandleMongoRequestRateTooLargeRetryExpiredException_withRetryAfter() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("DELETE");
        when(request.getRequestURI()).thenReturn("/delete/resource");

        MongoRequestRateTooLargeRetryExpiredException ex = mock(MongoRequestRateTooLargeRetryExpiredException.class);
        when(ex.getRetryAfterMs()).thenReturn(2700L);
        when(ex.getMessage()).thenReturn("Retry expired");

        ResponseEntity<ErrorDTO> response = handler.handleMongoRequestRateTooLargeRetryExpiredException(ex, request);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertEquals("TOO_MANY_REQUESTS", response.getBody().getCode());
        assertEquals("Too Many Requests", response.getBody().getMessage());
        assertEquals("3", response.getHeaders().getFirst(HttpHeaders.RETRY_AFTER)); // 2700 ms -> 3 s
        assertEquals("2700", response.getHeaders().getFirst("Retry-After-Ms"));
    }

    @Test
    void testGetRequestDetails_shouldReturnMethodAndURI() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("PUT");
        when(request.getRequestURI()).thenReturn("/update/resource");

        String details = MongoExceptionHandler.getRequestDetails(request);
        assertEquals("PUT /update/resource", details);
    }

}