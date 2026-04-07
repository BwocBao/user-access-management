package com.r2s.core.messaging.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserRegisteredEventTest {

    @Test
    void noArgsConstructor_andSetterGetter_shouldWork() {
        UserRegisteredEvent event = new UserRegisteredEvent();
        event.setUsername("bwocbao");

        assertEquals("bwocbao", event.getUsername());
    }

    @Test
    void allArgsConstructor_shouldWork() {
        UserRegisteredEvent event = new UserRegisteredEvent("admin1");

        assertEquals("admin1", event.getUsername());
    }

    @Test
    void builder_shouldWork() {
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .username("user123")
                .build();

        assertEquals("user123", event.getUsername());
    }
}