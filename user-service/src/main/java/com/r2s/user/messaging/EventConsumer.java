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

import java.util.Optional;

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
//        try {
//            throw new RuntimeException("Fake error");
//        } catch (Exception e) {
//            long tag = message.getMessageProperties().getDeliveryTag();
//            // retry logic ở đây
//            Object retryObj = message.getMessageProperties()
//                    .getHeaders()
//                    .get(HEADER_CONSUME_RETRY_COUNT);
//
//            int retryCount = retryObj instanceof Integer ? (Integer) retryObj : 0;
//
//            if (retryCount < MAX_RETRY) {
//
//                log.warn("Retrying message, attempt {}", retryCount + 1);
//
//                Message newMessage = MessageBuilder
//                        .fromMessage(message)
//                        .setHeader(HEADER_CONSUME_RETRY_COUNT, retryCount + 1)
//                        .build();
//
//                // =========================
//                // SEND TO RETRY
//                // =========================
//                rabbitTemplate.send(
//                        USER_RETRY_EXCHANGE,
//                        USER_REGISTERED_RETRY_ROUTING,
//                        newMessage
//                );
//
//                channel.basicAck(tag, false);
//            }
//            else {
//                log.error("Max retry reached → send to DLQ: {}", event.getUsername());
////                channel.basicReject(tag, false); tự cho vào dlq theo binding mình đã viết ở userexchange
//                // =========================
//                // SEND TO DLQ
//                // =========================
//                rabbitTemplate.send(
//                        USER_DLQ_EXCHANGE,
//                        USER_REGISTERED_DLQ_ROUTING,
//                        message
//                );
//
//                channel.basicAck(tag, false); // không reject nữa
//            }
//        }
        long tag = message.getMessageProperties().getDeliveryTag();

        try {
            String msgId = message.getMessageProperties().getMessageId();

            log.info("Processing messageId={} username={}", msgId, event.getUsername());

            // =========================
            // IDEMPOTENT CHECK
            // =========================
            if (userRepository.existsByUsername(event.getUsername())) {
                channel.basicAck(tag, false);
                return;
            }

            UserProfile newUser = new UserProfile();
            newUser.setUsername(event.getUsername());
            newUser.setFullName("");
            newUser.setEmail("");

            userRepository.saveAndFlush(newUser);

            // ✅ ACK
            channel.basicAck(tag, false);

        }
        catch (DataIntegrityViolationException e) {
            log.warn("Duplicate user detected: {}", event.getUsername());
            channel.basicAck(tag, false);
        }
        catch (Exception e) {

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

                log.warn("Retrying message, attempt {}", retryCount + 1);

                Message newMessage = MessageBuilder
                        .fromMessage(message)
                        .setHeader(HEADER_CONSUME_RETRY_COUNT, retryCount + 1)
                        .build();

                // =========================
                // SEND TO RETRY
                // =========================
                rabbitTemplate.send(
                        USER_RETRY_EXCHANGE,
                        USER_REGISTERED_RETRY_ROUTING,
                        newMessage
                );

                channel.basicAck(tag, false);

            } else {

                log.error("Max retry reached → send to DLQ: {}", event.getUsername());

                // =========================
                // SEND TO DLQ
                // =========================
                rabbitTemplate.send(
                        USER_DLQ_EXCHANGE,
                        USER_REGISTERED_DLQ_ROUTING,
                        message
                );

                channel.basicAck(tag, false); // không reject nữa
            }
        }
    }
}