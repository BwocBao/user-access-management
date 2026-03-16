package com.r2s.core.exception;

import com.r2s.core.dto.ApiResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleCustomException_shouldReturn400() {
        CustomException ex = new CustomException("Custom error");

        ResponseEntity<ApiResponse<?>> response = handler.handleCustomException(ex);

        assertEquals(400, response.getStatusCode().value());
        Assertions.assertNotNull(response.getBody());
        assertEquals("Custom error", response.getBody().getMessage());
    }

    @Test
    void handleAccessDenied_shouldReturn403() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");

        ResponseEntity<ApiResponse<?>> response = handler.handleAccessDenied(ex);

        assertEquals(403, response.getStatusCode().value());
        Assertions.assertNotNull(response.getBody());
        assertEquals("Forbidden", response.getBody().getMessage());
    }

    @Test
    void handleResourceNotFound_shouldReturn404() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Not found");

        ResponseEntity<ApiResponse<?>> response = handler.handleResourceNotFound(ex);

        assertEquals(404, response.getStatusCode().value());
        Assertions.assertNotNull(response.getBody());
        assertEquals("Not found", response.getBody().getMessage());
    }

    @Test
    void handleException_shouldReturn500() {
        Exception ex = new RuntimeException("Unexpected");

        ResponseEntity<ApiResponse<?>> response = handler.handleException(ex);

        assertEquals(500, response.getStatusCode().value());
        Assertions.assertNotNull(response.getBody());
        assertEquals("Internal server error", response.getBody().getMessage());
    }

    @Test
    void handleAuth_shouldReturn401() {
        AuthenticationException ex = new AuthenticationException("Auth error") {};

        ResponseEntity<ApiResponse<?>> response = handler.handleAuth(ex);

        assertEquals(401, response.getStatusCode().value());
        Assertions.assertNotNull(response.getBody());
        assertEquals("Unauthorized", response.getBody().getMessage());
    }

    @Test
    void handleUnauthorized_shouldReturn401() {
        UnAuthorizedException ex = new UnAuthorizedException("Unauthorized access");

        ResponseEntity<ApiResponse<?>> response = handler.handleUnauthorized(ex);

        assertEquals(401, response.getStatusCode().value());
        Assertions.assertNotNull(response.getBody());
        assertEquals("Unauthorized access", response.getBody().getMessage());
    }

    @Test
    void handleForbidden_shouldReturn403() {
        ForbiddenException ex = new ForbiddenException("Forbidden action");

        ResponseEntity<ApiResponse<?>> response = handler.handleForbidden(ex);

        assertEquals(403, response.getStatusCode().value());
        Assertions.assertNotNull(response.getBody());
        assertEquals("Forbidden action", response.getBody().getMessage());
    }

    @Test
    void handleValidation_shouldReturn400() {
        MethodArgumentNotValidException ex =
                new MethodArgumentNotValidException(null, new org.springframework.validation.BeanPropertyBindingResult(new Object(), "object"));

        ResponseEntity<ApiResponse<?>> response = handler.handleValidation(ex);

        assertEquals(400, response.getStatusCode().value());
        Assertions.assertNotNull(response.getBody());
        assertEquals("Validation failed", response.getBody().getMessage());
    }
}
