package com.r2s.user.messaging;

import com.r2s.core.messaging.CustomCorrelationData;
import com.r2s.core.messaging.event.UserDeletedEvent;
import com.r2s.core.messaging.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static com.r2s.core.messaging.RabbitConstants.*;

@Service
@RequiredArgsConstructor
public class EventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishUserDeleted(String username) {

        UserDeletedEvent event = new UserDeletedEvent(username);

        CustomCorrelationData cd =
                new CustomCorrelationData(USER_EXCHANGE, USER_DELETED_ROUTING);

        rabbitTemplate.convertAndSend(
                USER_EXCHANGE,
                USER_DELETED_ROUTING,
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
