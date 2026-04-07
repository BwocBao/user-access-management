package com.r2s.user.unit.messaging;

import com.rabbitmq.client.Channel;
import com.r2s.core.messaging.event.UserRegisteredEvent;
import com.r2s.user.entity.UserProfile;
import com.r2s.user.messaging.EventConsumer;
import com.r2s.user.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import static com.r2s.core.messaging.RabbitConstants.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventConsumerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private UserProfileRepository userRepository;

    @Mock
    private Channel channel;

    @InjectMocks
    private EventConsumer eventConsumer;

    private UserRegisteredEvent event;
    private Message message;

    @BeforeEach
    void setUp() {
        event = UserRegisteredEvent.builder()
                .username("bwocbao")
                .build();

        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(1L);
        props.setMessageId("msg-1");
        message = new Message("hello".getBytes(), props);
    }

    @Test
    void handle_shouldAckAndReturn_whenUserAlreadyExists() throws Exception {
        when(userRepository.existsByUsername("bwocbao")).thenReturn(true);

        eventConsumer.handle(event, message, channel);

        verify(userRepository).existsByUsername("bwocbao");
        verify(channel).basicAck(1L, false);
        verifyNoMoreInteractions(userRepository, channel);
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void handle_shouldCreateUserAndAck_whenUserDoesNotExist() throws Exception {
        when(userRepository.existsByUsername("bwocbao")).thenReturn(false);

        ArgumentCaptor<UserProfile> userCaptor =
                ArgumentCaptor.forClass(UserProfile.class);

        when(userRepository.saveAndFlush(userCaptor.capture()))
                .thenAnswer(inv -> inv.getArgument(0));

        eventConsumer.handle(event, message, channel);

        UserProfile savedUser = userCaptor.getValue();
        assertNotNull(savedUser);
        assertEquals("bwocbao", savedUser.getUsername());
        assertEquals("", savedUser.getFullName());
        assertEquals("", savedUser.getEmail());

        verify(userRepository).existsByUsername("bwocbao");
        verify(userRepository).saveAndFlush(any(UserProfile.class));
        verify(channel).basicAck(1L, false);
        verifyNoMoreInteractions(userRepository, channel);
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void handle_shouldAck_whenDuplicateUserDetected() throws Exception {
        when(userRepository.existsByUsername("bwocbao")).thenReturn(false);
        when(userRepository.saveAndFlush(any(UserProfile.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        eventConsumer.handle(event, message, channel);

        verify(userRepository).existsByUsername("bwocbao");
        verify(userRepository).saveAndFlush(any(UserProfile.class));
        verify(channel).basicAck(1L, false);
        verifyNoMoreInteractions(userRepository, channel);
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void handle_shouldSendToRetry_whenExceptionAndRetryLessThanMax() throws Exception {
        when(userRepository.existsByUsername("bwocbao")).thenReturn(false);
        when(userRepository.saveAndFlush(any(UserProfile.class)))
                .thenThrow(new RuntimeException("db error"));

        eventConsumer.handle(event, message, channel);

        ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

        verify(rabbitTemplate).send(
                eq(USER_RETRY_EXCHANGE),
                eq(USER_REGISTERED_RETRY_ROUTING),
                messageCaptor.capture()
        );

        Message retryMessage = messageCaptor.getValue();
        assertEquals(
                1,
                retryMessage.getMessageProperties().getHeaders().get(HEADER_CONSUME_RETRY_COUNT)
        );

        verify(channel).basicAck(1L, false);
    }

    @Test
    void handle_shouldSendToDlq_whenExceptionAndRetryReachedMax() throws Exception {
        when(userRepository.existsByUsername("bwocbao")).thenReturn(false);
        when(userRepository.saveAndFlush(any(UserProfile.class)))
                .thenThrow(new RuntimeException("db error"));

        message.getMessageProperties().setHeader(HEADER_CONSUME_RETRY_COUNT, MAX_RETRY);

        eventConsumer.handle(event, message, channel);

        verify(rabbitTemplate).send(
                USER_DLQ_EXCHANGE,
                USER_REGISTERED_DLQ_ROUTING,
                message
        );

        verify(channel).basicAck(1L, false);
    }

    @Test
    void extractRetryCount_shouldReturnHeaderValue_whenHeaderIsNumber() {
        message.getMessageProperties().setHeader(HEADER_CONSUME_RETRY_COUNT, 2);

        Integer retryCount = ReflectionTestUtils.invokeMethod(
                eventConsumer,
                "extractRetryCount",
                message
        );

        assertEquals(2, retryCount);
    }

    @Test
    void extractRetryCount_shouldReturnZero_whenHeaderMissing() {
        Integer retryCount = ReflectionTestUtils.invokeMethod(
                eventConsumer,
                "extractRetryCount",
                message
        );

        assertEquals(0, retryCount);
    }

    @Test
    void extractRetryCount_shouldReturnZero_whenHeaderIsNotNumber() {
        message.getMessageProperties().setHeader(HEADER_CONSUME_RETRY_COUNT, "abc");

        Integer retryCount = ReflectionTestUtils.invokeMethod(
                eventConsumer,
                "extractRetryCount",
                message
        );

        assertEquals(0, retryCount);
    }
}