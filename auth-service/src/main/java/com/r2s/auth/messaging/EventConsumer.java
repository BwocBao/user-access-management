package com.r2s.auth.messaging;

import com.r2s.auth.repository.UserRepository;
import com.r2s.core.messaging.event.UserDeletedEvent;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.r2s.core.messaging.RabbitConstants.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventConsumer {

    /*
     * RabbitTemplate dùng để gửi message tới:
     * - retry exchange
     * - DLQ exchange
     */
    private final RabbitTemplate rabbitTemplate;

    private final UserRepository userRepository;

    @RabbitListener(queues = USER_DELETED_QUEUE)
    @Transactional
    public void handle(UserDeletedEvent event,
                       Message message,
                       Channel channel) throws Exception {

        /*
         * deliveryTag là ID của message trong RabbitMQ channel.
         * dùng để ack message.
         */
        long tag = message.getMessageProperties().getDeliveryTag();

        String username = event.getUsername();

        /*
         * messageId giúp trace log và xử lý duplicate message.
         */
        String messageId = message.getMessageProperties().getMessageId();

        try {

            log.info(
                    "Processing user delete event: messageId={}, username={}",
                    messageId,
                    username
            );

            /*
             * Idempotency check:
             * nếu user đã bị xóa rồi thì skip.
             * trường hợp message bị gửi duplicate.
             */
            if (!userRepository.existsByUsername(username)) {

                log.warn(
                        "User already deleted, skip: messageId={}, username={}",
                        messageId,
                        username
                );

                /*
                 * ack message để RabbitMQ xóa message khỏi queue.
                 */
                channel.basicAck(tag, false);
                return;
            }

            userRepository.deleteByUsername(username);

            log.info(
                    "User deleted successfully in auth-service: messageId={}, username={}",
                    messageId,
                    username
            );

            /*
             * xử lý thành công -> ack message
             */
            channel.basicAck(tag, false);

        } catch (Exception e) {

            /*
             * lấy số lần retry hiện tại từ header
             */
            int retryCount = extractRetryCount(message);

            /*
             * nếu chưa vượt quá số lần retry cho phép
             */
            if (retryCount < MAX_RETRY) {

                int nextRetry = retryCount + 1;

                log.warn(
                        "Delete consume failed, send to retry exchange: attempt={}, messageId={}, username={}, error={}",
                        nextRetry,
                        messageId,
                        username,
                        e.getMessage()
                );

                /*
                 * tạo message mới với retry count tăng lên
                 */
                Message retryMessage = MessageBuilder
                        .fromMessage(message)
                        .setHeader(HEADER_CONSUME_RETRY_COUNT, nextRetry)
                        .build();

                /*
                 * gửi message sang retry exchange
                 * retry exchange thường có TTL:
                 * message sẽ delay trước khi quay lại queue chính.
                 */
                rabbitTemplate.send(
                        USER_RETRY_EXCHANGE,
                        USER_DELETED_RETRY_ROUTING,
                        retryMessage
                );

                /*
                 * ack message cũ để tránh bị consume lại ngay lập tức
                 */
                channel.basicAck(tag, false);

                return;
            }

            /*
             * nếu retry vượt quá MAX_RETRY -> gửi vào DLQ
             */
            log.error(
                    "Max retry reached, send delete event to DLQ: messageId={}, username={}, error={}",
                    messageId,
                    username,
                    e.getMessage(),
                    e
            );

            /*
             * gửi message sang Dead Letter Queue
             */
            rabbitTemplate.send(
                    USER_DLQ_EXCHANGE,
                    USER_DELETED_DLQ_ROUTING,
                    message
            );

            /*
             * ack message để remove khỏi queue chính
             */
            channel.basicAck(tag, false);
        }
    }

    /*
     * lấy retry count từ message header
     * nếu chưa có header -> mặc định = 0
     */
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