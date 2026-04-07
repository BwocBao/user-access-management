package com.r2s.core.exception;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResourceNotFoundExceptionTest {

    @Test
    void constructor_shouldSetMessage() {
        ResourceNotFoundException exception =
                new ResourceNotFoundException("User not found");

        assertEquals("User not found", exception.getMessage());
    }
}