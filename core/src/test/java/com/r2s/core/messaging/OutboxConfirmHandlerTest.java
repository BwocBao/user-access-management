package com.r2s.core.messaging;

import com.r2s.core.entity.Outbox;
import com.r2s.core.repository.OutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;

import java.util.Optional;

import static com.r2s.core.messaging.RabbitConstants.MAX_RETRY;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxConfirmHandlerTest {

    @Mock
    private OutboxRepository outboxRepository;

    @InjectMocks
    private OutboxConfirmHandler outboxConfirmHandler;

    private CustomCorrelationData customCorrelationData;

    @BeforeEach
    void setUp() {
        customCorrelationData = new CustomCorrelationData("user.exchange", "user.registered");
        customCorrelationData.setOutboxId("outbox-1");
    }

    @Test
    void handleConfirm_shouldIgnore_whenCorrelationDataIsNotCustomCorrelationData() {
        CorrelationData correlationData = new CorrelationData("normal-id");

        outboxConfirmHandler.handleConfirm(correlationData, true, null);

        verifyNoInteractions(outboxRepository);
    }

    @Test
    void handleConfirm_shouldIgnore_whenCorrelationDataIsNull() {
        outboxConfirmHandler.handleConfirm(null, true, null);

        verifyNoInteractions(outboxRepository);
    }

    @Test
    void handleConfirm_shouldIgnore_whenOutboxIdIsNull() {
        customCorrelationData.setOutboxId(null);

        outboxConfirmHandler.handleConfirm(customCorrelationData, true, null);

        verifyNoInteractions(outboxRepository);
    }

    @Test
    void handleConfirm_shouldMarkAsSent_whenAckTrueAndUpdateSuccess() {
        when(outboxRepository.markAsSent("outbox-1")).thenReturn(1);

        outboxConfirmHandler.handleConfirm(customCorrelationData, true, null);

        verify(outboxRepository).markAsSent("outbox-1");
        verify(outboxRepository, never()).findById(anyString());
        verify(outboxRepository, never()).increaseRetryAndRequeue(anyString());
        verify(outboxRepository, never()).markAsFailed(anyString());
    }

    @Test
    void handleConfirm_shouldOnlyCallMarkAsSent_whenAckTrueAndUpdateZero() {
        when(outboxRepository.markAsSent("outbox-1")).thenReturn(0);

        outboxConfirmHandler.handleConfirm(customCorrelationData, true, null);

        verify(outboxRepository).markAsSent("outbox-1");
        verify(outboxRepository, never()).findById(anyString());
        verify(outboxRepository, never()).increaseRetryAndRequeue(anyString());
        verify(outboxRepository, never()).markAsFailed(anyString());
    }

    @Test
    void handleConfirm_shouldIgnoreCause_whenAckTrue() {
        when(outboxRepository.markAsSent("outbox-1")).thenReturn(1);

        outboxConfirmHandler.handleConfirm(customCorrelationData, true, "some-cause");

        verify(outboxRepository).markAsSent("outbox-1");
        verify(outboxRepository, never()).findById(anyString());
    }

    @Test
    void handleConfirm_shouldDoNothing_whenAckFalseAndOutboxNotFound() {
        when(outboxRepository.findById("outbox-1")).thenReturn(Optional.empty());

        outboxConfirmHandler.handleConfirm(customCorrelationData, false, "broker error");

        verify(outboxRepository).findById("outbox-1");
        verify(outboxRepository, never()).increaseRetryAndRequeue(anyString());
        verify(outboxRepository, never()).markAsFailed(anyString());
        verify(outboxRepository, never()).markAsSent(anyString());
    }

    @Test
    void handleConfirm_shouldRequeue_whenAckFalseAndRetryNotExceeded() {
        Outbox outbox = new Outbox();
        outbox.setId("outbox-1");
        outbox.setRetryCount(0);

        when(outboxRepository.findById("outbox-1")).thenReturn(Optional.of(outbox));
        when(outboxRepository.increaseRetryAndRequeue("outbox-1")).thenReturn(1);

        outboxConfirmHandler.handleConfirm(customCorrelationData, false, "broker error");

        verify(outboxRepository).findById("outbox-1");
        verify(outboxRepository).increaseRetryAndRequeue("outbox-1");
        verify(outboxRepository, never()).markAsFailed(anyString());
        verify(outboxRepository, never()).markAsSent(anyString());
    }

    @Test
    void handleConfirm_shouldWarnOnly_whenAckFalseAndRequeueUpdateZero() {
        Outbox outbox = new Outbox();
        outbox.setId("outbox-1");
        outbox.setRetryCount(0);

        when(outboxRepository.findById("outbox-1")).thenReturn(Optional.of(outbox));
        when(outboxRepository.increaseRetryAndRequeue("outbox-1")).thenReturn(0);

        outboxConfirmHandler.handleConfirm(customCorrelationData, false, "broker error");

        verify(outboxRepository).findById("outbox-1");
        verify(outboxRepository).increaseRetryAndRequeue("outbox-1");
        verify(outboxRepository, never()).markAsFailed(anyString());
    }

    @Test
    void handleConfirm_shouldMarkAsFailed_whenAckFalseAndRetryExceeded() {
        Outbox outbox = new Outbox();
        outbox.setId("outbox-1");
        outbox.setRetryCount(MAX_RETRY - 1);

        when(outboxRepository.findById("outbox-1")).thenReturn(Optional.of(outbox));

        outboxConfirmHandler.handleConfirm(customCorrelationData, false, "broker error");

        verify(outboxRepository).findById("outbox-1");
        verify(outboxRepository).markAsFailed("outbox-1");
        verify(outboxRepository, never()).increaseRetryAndRequeue(anyString());
        verify(outboxRepository, never()).markAsSent(anyString());
    }

    @Test
    void handleReturned_shouldIgnore_whenMessageIdIsNull() {
        MessageProperties props = new MessageProperties();
        Message message = new Message("test".getBytes(), props);

        ReturnedMessage returned = new ReturnedMessage(
                message,
                312,
                "NO_ROUTE",
                "user.exchange",
                "user.registered"
        );

        outboxConfirmHandler.handleReturned(returned);

        verifyNoInteractions(outboxRepository);
    }

    @Test
    void handleReturned_shouldDoNothing_whenOutboxNotFound() {
        MessageProperties props = new MessageProperties();
        props.setMessageId("outbox-1");
        Message message = new Message("test".getBytes(), props);

        ReturnedMessage returned = new ReturnedMessage(
                message,
                312,
                "NO_ROUTE",
                "user.exchange",
                "user.registered"
        );

        when(outboxRepository.findById("outbox-1")).thenReturn(Optional.empty());

        outboxConfirmHandler.handleReturned(returned);

        verify(outboxRepository).findById("outbox-1");
        verify(outboxRepository, never()).increaseRetryAndRequeue(anyString());
        verify(outboxRepository, never()).markAsFailed(anyString());
    }

    @Test
    void handleReturned_shouldRequeue_whenRetryNotExceeded() {
        MessageProperties props = new MessageProperties();
        props.setMessageId("outbox-1");
        Message message = new Message("test".getBytes(), props);

        ReturnedMessage returned = new ReturnedMessage(
                message,
                312,
                "NO_ROUTE",
                "user.exchange",
                "user.registered"
        );

        Outbox outbox = new Outbox();
        outbox.setId("outbox-1");
        outbox.setRetryCount(1);

        when(outboxRepository.findById("outbox-1")).thenReturn(Optional.of(outbox));
        when(outboxRepository.increaseRetryAndRequeue("outbox-1")).thenReturn(1);

        outboxConfirmHandler.handleReturned(returned);

        verify(outboxRepository).findById("outbox-1");
        verify(outboxRepository).increaseRetryAndRequeue("outbox-1");
        verify(outboxRepository, never()).markAsFailed(anyString());
    }

    @Test
    void handleReturned_shouldMarkAsFailed_whenRetryExceeded() {
        MessageProperties props = new MessageProperties();
        props.setMessageId("outbox-1");
        Message message = new Message("test".getBytes(), props);

        ReturnedMessage returned = new ReturnedMessage(
                message,
                312,
                "NO_ROUTE",
                "user.exchange",
                "user.registered"
        );

        Outbox outbox = new Outbox();
        outbox.setId("outbox-1");
        outbox.setRetryCount(MAX_RETRY - 1);

        when(outboxRepository.findById("outbox-1")).thenReturn(Optional.of(outbox));

        outboxConfirmHandler.handleReturned(returned);

        verify(outboxRepository).findById("outbox-1");
        verify(outboxRepository).markAsFailed("outbox-1");
        verify(outboxRepository, never()).increaseRetryAndRequeue(anyString());
    }
}