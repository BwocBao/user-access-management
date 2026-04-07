package com.r2s.user.unit.messaging;

import com.r2s.user.messaging.RabbitListenerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RabbitListenerConfigTest {

    private final RabbitListenerConfig config = new RabbitListenerConfig();

    @Test
    void rabbitListenerContainerFactory_shouldBeConfiguredCorrectly() {
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();

        SimpleRabbitListenerContainerFactory factory =
                config.rabbitListenerContainerFactory(connectionFactory, converter);

        assertNotNull(factory);

        Object actualConnectionFactory =
                ReflectionTestUtils.getField(factory, "connectionFactory");
        Object actualMessageConverter =
                ReflectionTestUtils.getField(factory, "messageConverter");
        Object actualAcknowledgeMode =
                ReflectionTestUtils.getField(factory, "acknowledgeMode");
        Object actualConcurrentConsumers =
                ReflectionTestUtils.getField(factory, "concurrentConsumers");
        Object actualMaxConcurrentConsumers =
                ReflectionTestUtils.getField(factory, "maxConcurrentConsumers");
        Object actualPrefetchCount =
                ReflectionTestUtils.getField(factory, "prefetchCount");

        assertEquals(connectionFactory, actualConnectionFactory);
        assertEquals(converter, actualMessageConverter);
        assertEquals(AcknowledgeMode.MANUAL, actualAcknowledgeMode);
        assertEquals(3, actualConcurrentConsumers);
        assertEquals(5, actualMaxConcurrentConsumers);
        assertEquals(1, actualPrefetchCount);
    }
}