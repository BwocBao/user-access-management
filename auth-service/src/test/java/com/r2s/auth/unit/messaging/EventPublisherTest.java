package com.r2s.auth.unit.messaging;


import com.r2s.auth.messaging.EventPublisher;
import com.r2s.core.messaging.CustomCorrelationData;
import com.r2s.core.messaging.event.UserRegisteredEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static com.r2s.core.messaging.RabbitConstants.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EventPublisherTest {

    private RabbitTemplate rabbitTemplate;
    private EventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        rabbitTemplate = mock(RabbitTemplate.class);
        eventPublisher = new EventPublisher(rabbitTemplate);
    }

    @Test
    void publishUserRegistered_shouldSendCorrectEvent() {
        eventPublisher.publishUserRegistered("bwocbao");

        ArgumentCaptor<UserRegisteredEvent> eventCaptor =
                ArgumentCaptor.forClass(UserRegisteredEvent.class);
        ArgumentCaptor<MessagePostProcessor> processorCaptor =
                ArgumentCaptor.forClass(MessagePostProcessor.class);
        ArgumentCaptor<CustomCorrelationData> correlationCaptor =
                ArgumentCaptor.forClass(CustomCorrelationData.class);

        verify(rabbitTemplate).convertAndSend(
                eq(USER_EXCHANGE),
                eq(USER_REGISTERED_ROUTING),
                eventCaptor.capture(),
                processorCaptor.capture(),
                correlationCaptor.capture()
        );

        assertEquals("bwocbao", eventCaptor.getValue().getUsername());

        CustomCorrelationData correlationData = correlationCaptor.getValue();
        assertEquals(USER_EXCHANGE, correlationData.getExchange());
        assertEquals(USER_REGISTERED_ROUTING, correlationData.getRoutingKey());
    }

    @Test
    void publishFakeUserRegistered_shouldSendToFakeExchange() {
        eventPublisher.publishFakeUserRegistered("fakeUser");

        ArgumentCaptor<UserRegisteredEvent> eventCaptor =
                ArgumentCaptor.forClass(UserRegisteredEvent.class);
        ArgumentCaptor<MessagePostProcessor> processorCaptor =
                ArgumentCaptor.forClass(MessagePostProcessor.class);
        ArgumentCaptor<CustomCorrelationData> correlationCaptor =
                ArgumentCaptor.forClass(CustomCorrelationData.class);

        verify(rabbitTemplate).convertAndSend(
                eq("fake.exchange"),
                eq(USER_REGISTERED_ROUTING),
                eventCaptor.capture(),
                processorCaptor.capture(),
                correlationCaptor.capture()
        );

        assertEquals("fakeUser", eventCaptor.getValue().getUsername());

        CustomCorrelationData correlationData = correlationCaptor.getValue();
        assertEquals("fake.exchange", correlationData.getExchange());
        assertEquals(USER_REGISTERED_ROUTING, correlationData.getRoutingKey());
    }

    @Test
    void enrichMessage_shouldSetHeadersMessageIdAndCorrelationMessage() throws Exception {
        CustomCorrelationData correlationData =
                new CustomCorrelationData(USER_EXCHANGE, USER_REGISTERED_ROUTING);

        Message message = new Message("hello".getBytes(), new MessageProperties());

        Message result = ReflectionTestUtils.invokeMethod(
                eventPublisher,
                "enrichMessage",
                message,
                correlationData
        );

        assertNotNull(result);
        assertEquals(0, result.getMessageProperties().getHeaders().get(HEADER_PUBLISH_RETRY_COUNT));
        assertNotNull(result.getMessageProperties().getMessageId());
        assertEquals(result, correlationData.getMessage());
    }
}