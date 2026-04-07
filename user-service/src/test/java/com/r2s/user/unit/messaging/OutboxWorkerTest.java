package com.r2s.user.unit.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.core.entity.Outbox;
import com.r2s.core.messaging.CustomCorrelationData;
import com.r2s.core.messaging.event.UserDeletedEvent;
import com.r2s.core.repository.OutboxRepository;
import com.r2s.user.messaging.OutboxWorker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static com.r2s.core.messaging.RabbitConstants.MAX_RETRY;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxWorkerTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private OutboxWorker outboxWorker;

    private Outbox outbox;

    @BeforeEach
    void setUp() {
        outbox = new Outbox();
        outbox.setId("outbox-1");
        outbox.setStatus("PENDING");
        outbox.setRetryCount(0);
        outbox.setExchange("user.exchange");
        outbox.setRoutingKey("user.deleted");
        outbox.setPayload("{\"username\":\"haruki\"}");
        outbox.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void process_shouldMarkFailed_whenRetryExceeded() {
        outbox.setRetryCount(MAX_RETRY);

        when(outboxRepository.findTop10ByStatusOrderByCreatedAtAsc("PENDING"))
                .thenReturn(List.of(outbox));

        outboxWorker.process();

        verify(outboxRepository).markAsFailed("outbox-1");
        verify(outboxRepository, never()).markAsProcessing(anyString());
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void process_shouldSkip_whenLockFailed() {
        when(outboxRepository.findTop10ByStatusOrderByCreatedAtAsc("PENDING"))
                .thenReturn(List.of(outbox));
        when(outboxRepository.markAsProcessing("outbox-1")).thenReturn(0);

        outboxWorker.process();

        verify(outboxRepository).markAsProcessing("outbox-1");
        verifyNoInteractions(objectMapper);
        verifyNoInteractions(rabbitTemplate);
        verify(outboxRepository, never()).markAsFailed(anyString());
    }

    @Test
    void process_shouldPublishSuccessfully() throws Exception {
        UserDeletedEvent event = new UserDeletedEvent();

        when(outboxRepository.findTop10ByStatusOrderByCreatedAtAsc("PENDING"))
                .thenReturn(List.of(outbox));
        when(outboxRepository.markAsProcessing("outbox-1")).thenReturn(1);
        when(objectMapper.readValue(outbox.getPayload(), UserDeletedEvent.class))
                .thenReturn(event);

        outboxWorker.process();

        ArgumentCaptor<MessagePostProcessor> mppCaptor =
                ArgumentCaptor.forClass(MessagePostProcessor.class);
        ArgumentCaptor<CustomCorrelationData> cdCaptor =
                ArgumentCaptor.forClass(CustomCorrelationData.class);

        verify(rabbitTemplate).convertAndSend(
                eq("user.exchange"),
                eq("user.deleted"),
                eq(event),
                mppCaptor.capture(),
                cdCaptor.capture()
        );

        CustomCorrelationData cd = cdCaptor.getValue();
        assertNotNull(cd);
        assertEquals("outbox-1", ReflectionTestUtils.getField(cd, "outboxId"));

        MessagePostProcessor processor = mppCaptor.getValue();
        Message message = new Message("{}".getBytes(StandardCharsets.UTF_8));

        Message processed = processor.postProcessMessage(message);

        assertEquals("outbox-1", processed.getMessageProperties().getMessageId());

        Object storedMessage = ReflectionTestUtils.getField(cd, "message");
        assertNotNull(storedMessage);
    }

    @Test
    void process_shouldIncreaseRetryAndRequeue_whenObjectMapperFailsAndRetryNotExceeded() throws Exception {
        outbox.setRetryCount(0);

        when(outboxRepository.findTop10ByStatusOrderByCreatedAtAsc("PENDING"))
                .thenReturn(List.of(outbox));
        when(outboxRepository.markAsProcessing("outbox-1")).thenReturn(1);
        when(objectMapper.readValue(outbox.getPayload(), UserDeletedEvent.class))
                .thenThrow(new RuntimeException("json error"));

        outboxWorker.process();

        verify(outboxRepository).increaseRetryAndRequeue("outbox-1");
        verify(outboxRepository, never()).markAsFailed("outbox-1");
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void process_shouldMarkFailed_whenLocalErrorAndNextRetryExceeded() throws Exception {
        outbox.setRetryCount(MAX_RETRY - 1);

        when(outboxRepository.findTop10ByStatusOrderByCreatedAtAsc("PENDING"))
                .thenReturn(List.of(outbox));
        when(outboxRepository.markAsProcessing("outbox-1")).thenReturn(1);
        when(objectMapper.readValue(outbox.getPayload(), UserDeletedEvent.class))
                .thenThrow(new RuntimeException("json error"));

        outboxWorker.process();

        verify(outboxRepository).markAsFailed("outbox-1");
        verify(outboxRepository, never()).increaseRetryAndRequeue("outbox-1");
        verifyNoInteractions(rabbitTemplate);
    }

    @Test
    void process_shouldIncreaseRetryAndRequeue_whenRabbitPublishFails() throws Exception {
        UserDeletedEvent event = new UserDeletedEvent();

        when(outboxRepository.findTop10ByStatusOrderByCreatedAtAsc("PENDING"))
                .thenReturn(List.of(outbox));
        when(outboxRepository.markAsProcessing("outbox-1")).thenReturn(1);
        when(objectMapper.readValue(outbox.getPayload(), UserDeletedEvent.class))
                .thenReturn(event);

        doThrow(new RuntimeException("rabbit publish fail"))
                .when(rabbitTemplate)
                .convertAndSend(
                        anyString(),
                        anyString(),
                        any(),
                        any(MessagePostProcessor.class),
                        any(CustomCorrelationData.class)
                );

        outboxWorker.process();

        verify(outboxRepository).increaseRetryAndRequeue("outbox-1");
        verify(outboxRepository, never()).markAsFailed("outbox-1");
    }

    @Test
    void init_shouldNotThrowException() {
        assertDoesNotThrow(() -> outboxWorker.init());
    }
}