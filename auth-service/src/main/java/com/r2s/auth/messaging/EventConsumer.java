package com.r2s.auth.messaging;


import com.r2s.auth.repository.UserRepository;
import com.r2s.core.messaging.event.UserDeletedEvent;
import com.r2s.core.messaging.event.UserRegisteredEvent;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.r2s.core.messaging.RabbitConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventConsumer {

    private final RabbitTemplate rabbitTemplate;
    private final UserRepository userRepository;

    @RabbitListener(queues = USER_DELETED_QUEUE)
    @Transactional
    public void handle(UserDeletedEvent event,
                       Message message,
                       Channel channel) throws Exception {

        long tag = message.getMessageProperties().getDeliveryTag();

        try {
            String username = event.getUsername();
            String msgId = message.getMessageProperties().getMessageId();

            log.info("Deleting user: messageId={} username={}", msgId, username);

            // =========================
            // IDEMPOTENT CHECK
            // =========================
            if (!userRepository.existsByUsername(username)) {
                log.warn("User already deleted: {}", username);
                channel.basicAck(tag, false);
                return;
            }

            // =========================
            // DELETE USER
            // =========================
            userRepository.deleteByUsername(username);

            // ✅ ACK
            channel.basicAck(tag, false);

        } catch (Exception e) {
            log.error("ERROR processing user delete", e);
            // =========================
            // RETRY COUNT
            // =========================
            Object retryObj = message.getMessageProperties()
                    .getHeaders()
                    .get(HEADER_CONSUME_RETRY_COUNT);

            int retryCount = Optional.ofNullable(retryObj)
                    .filter(Integer.class::isInstance)
                    .map(Integer.class::cast)
                    .orElse(0);

            if (retryCount < MAX_RETRY) {

                log.warn("Retry delete user {}, attempt {}", event.getUsername(), retryCount + 1);

                Message newMessage = MessageBuilder
                        .fromMessage(message)
                        .setHeader(HEADER_CONSUME_RETRY_COUNT, retryCount + 1)
                        .build();

                // =========================
                // SEND TO RETRY (đúng routing)
                // =========================
                rabbitTemplate.send(
                        USER_RETRY_EXCHANGE,
                        USER_DELETED_RETRY_ROUTING,
                        newMessage
                );

                channel.basicAck(tag, false);

            } else {

                log.error("Max retry reached → send to DLQ: {}", event.getUsername());

                // =========================
                // SEND TO DLQ (đúng routing)
                // =========================
                rabbitTemplate.send(
                        USER_DLQ_EXCHANGE,
                        USER_DELETED_DLQ_ROUTING,
                        message
                );

                channel.basicAck(tag, false);
            }
        }
    }
}