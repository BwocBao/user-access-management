package com.r2s.core.exception;

import com.r2s.core.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleCustomException_shouldReturn400() {
        CustomException ex = new CustomException("Custom error");

        ResponseEntity<ApiResponse<?>> response = handler.handleCustomException(ex);

        assertEquals(400, response.getStatusCode().value());
        assertEquals("Custom error", response.getBody().getMessage());
    }

    @Test
    void handleAccessDenied_shouldReturn403() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");

        ResponseEntity<ApiResponse<?>> response = handler.handleAccessDenied(ex);

        assertEquals(403, response.getStatusCode().value());
        assertEquals("Forbidden", response.getBody().getMessage());
    }

    @Test
    void handleResourceNotFound_shouldReturn404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Not found");

        ResponseEntity<ApiResponse<?>> response = handler.handleResourceNotFound(ex);

        assertEquals(404, response.getStatusCode().value());
        assertEquals("Not found", response.getBody().getMessage());
    }

    @Test
    void handleException_shouldReturn500() {
        Exception ex = new RuntimeException("Unexpected");

        ResponseEntity<ApiResponse<?>> response = handler.handleException(ex);

        assertEquals(500, response.getStatusCode().value());
        assertEquals("Internal server error", response.getBody().getMessage());
    }
}
