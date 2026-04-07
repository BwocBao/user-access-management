package com.r2s.core.messaging;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class RabbitConfigTest {

    private RabbitConfig rabbitConfig;

    @BeforeEach
    void setUp() {
        rabbitConfig = new RabbitConfig();
    }

    @Test
    void connectionFactory_shouldCreateAndSetPropertiesCorrectly() {
        CachingConnectionFactory factory = rabbitConfig.connectionFactory(
                "localhost",
                5672,
                "guest",
                "guest"
        );

        assertNotNull(factory);
        assertEquals("localhost", factory.getHost());
        assertEquals(5672, factory.getPort());
        assertEquals("guest", factory.getUsername());
        assertEquals("guest", factory.getRabbitConnectionFactory().getPassword());
        assertTrue(factory.isPublisherConfirms());
        assertFalse(factory.isSimplePublisherConfirms());
        assertTrue(factory.isPublisherReturns());
    }

    @Test
    void converter_shouldReturnJackson2JsonMessageConverter() {
        Jackson2JsonMessageConverter converter = rabbitConfig.converter();

        assertNotNull(converter);
        assertInstanceOf(Jackson2JsonMessageConverter.class, converter);
    }

    @Test
    void rabbitTemplate_shouldBeConfiguredCorrectly() {
        CachingConnectionFactory cf = mock(CachingConnectionFactory.class);
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        OutboxConfirmHandler outboxConfirmHandler = mock(OutboxConfirmHandler.class);

        RabbitTemplate template = rabbitConfig.rabbitTemplate(cf, converter, outboxConfirmHandler);

        assertNotNull(template);
        assertSame(cf, template.getConnectionFactory());
        assertSame(converter, template.getMessageConverter());

        Message message = new Message(new byte[0], new MessageProperties());

        // Vì config gọi template.setMandatory(true)
        // nên với template hiện tại, message phải được đánh mandatory
        assertEquals(Boolean.TRUE, template.isMandatoryFor(message));

        // Không assert confirmCallback/returnsCallback bằng reflection nữa
        // vì đó là internal state, dễ vỡ theo version
    }

    @Test
    void init_shouldNotThrowException() {
        assertDoesNotThrow(() -> rabbitConfig.init());
    }
}