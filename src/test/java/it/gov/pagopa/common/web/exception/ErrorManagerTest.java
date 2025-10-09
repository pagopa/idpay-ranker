package it.gov.pagopa.common.web.exception;

import it.gov.pagopa.common.web.dto.ErrorDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErrorManagerTest {
    private ErrorManager errorManager;
    private ErrorDTO customErrorDTO;

    @BeforeEach
    void setUp() {
        customErrorDTO = new ErrorDTO("CustomError", "Custom message");
        errorManager = new ErrorManager(customErrorDTO);
    }

    @Test
    void testConstructor_nullDefaultErrorDTO_shouldCreateDefault() {
        ErrorManager manager = new ErrorManager(null);
        ResponseEntity<ErrorDTO> response = manager.handleException(new RuntimeException(), mock(HttpServletRequest.class));
        assertEquals("Error", response.getBody().getCode());
        assertEquals("Something gone wrong", response.getBody().getMessage());
    }

    @Test
    void testHandleException_shouldReturnDefaultErrorDTO() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/test");

        RuntimeException exception = new RuntimeException("Test exception");

        ResponseEntity<ErrorDTO> response = errorManager.handleException(exception, request);

        assertEquals(500, response.getStatusCodeValue());
        assertEquals(customErrorDTO.getCode(), response.getBody().getCode());
        assertEquals(customErrorDTO.getMessage(), response.getBody().getMessage());
    }

    @Test
    void testLogClientException_shouldReturnFormattedMessage() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/example");

        RuntimeException exception = new RuntimeException("Test log");

        ErrorManager.logClientException(exception, request);
    }

    @Test
    void testGetRequestDetails_shouldReturnMethodAndURI() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("DELETE");
        when(request.getRequestURI()).thenReturn("/delete/resource");

        String details = ErrorManager.getRequestDetails(request);
        assertEquals("DELETE /delete/resource", details);
    }

}