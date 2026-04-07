package com.r2s.auth.messaging;

import com.r2s.core.messaging.CustomCorrelationData;
import com.r2s.core.messaging.event.UserRegisteredEvent;
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

    public void publishUserRegistered(String username) {
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .username(username)
                .build();

        CustomCorrelationData correlationData =
                new CustomCorrelationData(USER_EXCHANGE, USER_REGISTERED_ROUTING);

        rabbitTemplate.convertAndSend(
                USER_EXCHANGE,
                USER_REGISTERED_ROUTING,
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

    public void publishFakeUserRegistered(String username) {

        UserRegisteredEvent event = new UserRegisteredEvent(username);

        CustomCorrelationData cd =
                new CustomCorrelationData("fake.exchange", USER_REGISTERED_ROUTING);

        rabbitTemplate.convertAndSend(
                "fake.exchange",
                USER_REGISTERED_ROUTING,
                event,
                msg -> {

                    // ✅ publish retry count
                    msg.getMessageProperties()
                            .getHeaders()
                            .put(HEADER_PUBLISH_RETRY_COUNT, 0);

                    // ✅ messageId (idempotent support)
                    msg.getMessageProperties()
                            .setMessageId(UUID.randomUUID().toString());

                    cd.setMessage(msg);

                    return msg;
                },
                cd
        );
    }
}