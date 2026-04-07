package com.r2s.user.unit.messaging;

import com.r2s.user.messaging.RabbitConsumerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;

import java.util.Map;

import static com.r2s.core.messaging.RabbitConstants.*;
import static org.junit.jupiter.api.Assertions.*;

class RabbitConsumerConfigTest {

    private final RabbitConsumerConfig config = new RabbitConsumerConfig();

    @Test
    void userQueue_shouldBeCreatedCorrectly() {
        Queue queue = config.userQueue();

        assertNotNull(queue);
        assertEquals(USER_REGISTERED_QUEUE, queue.getName());
        assertTrue(queue.isDurable());

        Map<String, Object> args = queue.getArguments();
        assertEquals(USER_DLQ_EXCHANGE, args.get("x-dead-letter-exchange"));
        assertEquals(USER_REGISTERED_DLQ_ROUTING, args.get("x-dead-letter-routing-key"));
    }

    @Test
    void userBinding_shouldBeCreatedCorrectly() {
        Queue queue = config.userQueue();
        TopicExchange exchange = new TopicExchange(USER_EXCHANGE);

        Binding binding = config.userBinding(queue, exchange);

        assertNotNull(binding);
        assertEquals(USER_REGISTERED_QUEUE, binding.getDestination());
        assertEquals(USER_EXCHANGE, binding.getExchange());
        assertEquals(USER_REGISTERED_ROUTING, binding.getRoutingKey());
    }

    @Test
    void dlqQueue_shouldBeCreatedCorrectly() {
        Queue queue = config.dlqQueue();

        assertNotNull(queue);
        assertEquals(USER_REGISTERED_DLQ_QUEUE, queue.getName());
        assertTrue(queue.isDurable());
    }

    @Test
    void dlqBinding_shouldBeCreatedCorrectly() {
        Queue queue = config.dlqQueue();
        TopicExchange exchange = new TopicExchange(USER_DLQ_EXCHANGE);

        Binding binding = config.dlqBinding(queue, exchange);

        assertNotNull(binding);
        assertEquals(USER_REGISTERED_DLQ_QUEUE, binding.getDestination());
        assertEquals(USER_DLQ_EXCHANGE, binding.getExchange());
        assertEquals(USER_REGISTERED_DLQ_ROUTING, binding.getRoutingKey());
    }

    @Test
    void retryQueue_shouldBeCreatedCorrectly() {
        Queue queue = config.retryQueue();

        assertNotNull(queue);
        assertEquals(USER_REGISTERED_RETRY_QUEUE, queue.getName());
        assertTrue(queue.isDurable());

        Map<String, Object> args = queue.getArguments();
        assertEquals(USER_EXCHANGE, args.get("x-dead-letter-exchange"));
        assertEquals(USER_REGISTERED_ROUTING, args.get("x-dead-letter-routing-key"));
        assertEquals(RETRY_TTL_MS, args.get("x-message-ttl"));
    }

    @Test
    void retryBinding_shouldBeCreatedCorrectly() {
        Queue queue = config.retryQueue();
        TopicExchange exchange = new TopicExchange(USER_RETRY_EXCHANGE);

        Binding binding = config.retryBinding(queue, exchange);

        assertNotNull(binding);
        assertEquals(USER_REGISTERED_RETRY_QUEUE, binding.getDestination());
        assertEquals(USER_RETRY_EXCHANGE, binding.getExchange());
        assertEquals(USER_REGISTERED_RETRY_ROUTING, binding.getRoutingKey());
    }
}