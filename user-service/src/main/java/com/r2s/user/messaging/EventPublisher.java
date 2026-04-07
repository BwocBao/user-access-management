package com.r2s.user.messaging;

import com.r2s.core.messaging.CustomCorrelationData;
import com.r2s.core.messaging.event.UserDeletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.r2s.core.messaging.RabbitConstants.*;

@Service
@RequiredArgsConstructor
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishUserDeleted(String username) {
        UserDeletedEvent event = UserDeletedEvent.builder()
                .username(username)
                .build();

        CustomCorrelationData correlationData =
                new CustomCorrelationData(USER_EXCHANGE, USER_DELETED_ROUTING);

        rabbitTemplate.convertAndSend(
                USER_EXCHANGE,
                USER_DELETED_ROUTING,
                event,
                message -> enrichMessage(message, correlationData),
                correlationData
        );
    }

    private Message enrichMessage(Message message, CustomCorrelationData correlationData) {
        message.getMessageProperties()
                .getHeaders()
                .put(HEADER_PUBLISH_RETRY_COUNT, 0);

        message.getMessageProperties()
                .setMessageId(UUID.randomUUID().toString());

        correlationData.setMessage(message);
        return message;
    }
}