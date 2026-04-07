package com.r2s.user.messaging;

import com.r2s.core.messaging.event.UserRegisteredEvent;
import com.r2s.user.entity.UserProfile;
import com.r2s.user.repository.UserProfileRepository;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import static com.r2s.core.messaging.RabbitConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventConsumer {

    private final RabbitTemplate rabbitTemplate;
    private final UserProfileRepository userRepository;

    @RabbitListener(queues = USER_REGISTERED_QUEUE)
    public void handle(UserRegisteredEvent event,
                       Message message,
                       Channel channel) throws Exception {

        long tag = message.getMessageProperties().getDeliveryTag();
        String messageId = message.getMessageProperties().getMessageId();
        String username = event.getUsername();

        try {
            log.info("Processing user registered event: messageId={}, username={}", messageId, username);

            if (userRepository.existsByUsername(username)) {
                log.info("User profile already exists, skip create: messageId={}, username={}", messageId, username);
                channel.basicAck(tag, false);
                return;
            }

            UserProfile newUser = new UserProfile();
            newUser.setUsername(username);
            newUser.setFullName("");
            newUser.setEmail("");

            userRepository.saveAndFlush(newUser);

            log.info("User profile created successfully: messageId={}, username={}", messageId, username);
            channel.basicAck(tag, false);

        } catch (DataIntegrityViolationException e) {
            log.warn("Duplicate user detected, ack as idempotent success: messageId={}, username={}", messageId, username);
            channel.basicAck(tag, false);

        } catch (Exception e) {
            int retryCount = extractRetryCount(message);

            if (retryCount < MAX_RETRY) {
                int nextRetry = retryCount + 1;

                log.warn(
                        "Consume failed, send to retry exchange: attempt={}, messageId={}, username={}, error={}",
                        nextRetry, messageId, username, e.getMessage()
                );

                Message retryMessage = MessageBuilder
                        .fromMessage(message)
                        .setHeader(HEADER_CONSUME_RETRY_COUNT, nextRetry)
                        .build();

                rabbitTemplate.send(
                        USER_RETRY_EXCHANGE,
                        USER_REGISTERED_RETRY_ROUTING,
                        retryMessage
                );

                channel.basicAck(tag, false);
                return;
            }

            log.error(
                    "Max retry reached, send to DLQ: messageId={}, username={}, error={}",
                    messageId, username, e.getMessage(), e
            );

            rabbitTemplate.send(
                    USER_DLQ_EXCHANGE,
                    USER_REGISTERED_DLQ_ROUTING,
                    message
            );

            channel.basicAck(tag, false);
        }
    }

    private int extractRetryCount(Message message) {
        Object retryObj = message.getMessageProperties()
                .getHeaders()
                .get(HEADER_CONSUME_RETRY_COUNT);

        if (retryObj instanceof Number number) {
            return number.intValue();
        }

        return 0;
    }
}