package com.r2s.core.messaging.event;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserDeletedEventTest {

    @Test
    void noArgsConstructor_andSetterGetter_shouldWork() {
        UserDeletedEvent event = new UserDeletedEvent();
        event.setUsername("bwocbao");

        assertEquals("bwocbao", event.getUsername());
    }

    @Test
    void allArgsConstructor_shouldWork() {
        UserDeletedEvent event = new UserDeletedEvent("admin1");

        assertEquals("admin1", event.getUsername());
    }

    @Test
    void builder_shouldWork() {
        UserDeletedEvent event = UserDeletedEvent.builder()
                .username("user123")
                .build();

        assertEquals("user123", event.getUsername());
    }
}
