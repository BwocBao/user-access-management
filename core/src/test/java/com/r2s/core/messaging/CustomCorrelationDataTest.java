package com.r2s.core.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import static org.junit.jupiter.api.Assertions.*;

class CustomCorrelationDataTest {

    @Test
    void noArgsConstructor_andSetters_shouldWork() {
        CustomCorrelationData data = new CustomCorrelationData();

        Message message = new Message("hello".getBytes(), new MessageProperties());

        data.setExchange("user.exchange");
        data.setRoutingKey("user.registered.routing");
        data.setMessage(message);

        assertEquals("user.exchange", data.getExchange());
        assertEquals("user.registered.routing", data.getRoutingKey());
        assertEquals(message, data.getMessage());
    }

    @Test
    void twoArgsConstructor_shouldSetExchangeAndRoutingKey() {
        CustomCorrelationData data =
                new CustomCorrelationData("user.exchange", "user.deleted.routing");

        assertEquals("user.exchange", data.getExchange());
        assertEquals("user.deleted.routing", data.getRoutingKey());
        assertNull(data.getMessage());
    }

    @Test
    void allArgsConstructor_shouldSetAllFields() {
        Message message = new Message("payload".getBytes(), new MessageProperties());

        CustomCorrelationData data =
                new CustomCorrelationData("ex", "rk", message,"UUID");

        assertEquals("ex", data.getExchange());
        assertEquals("rk", data.getRoutingKey());
        assertEquals(message, data.getMessage());
        assertEquals("UUID", data.getOutboxId());
    }
}