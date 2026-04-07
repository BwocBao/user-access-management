package com.r2s.auth.messaging;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.r2s.core.messaging.RabbitConstants.*;
import static com.r2s.core.messaging.RabbitConstants.RETRY_TTL_MS;
import static com.r2s.core.messaging.RabbitConstants.USER_EXCHANGE;


/*
 * Class cấu hình queue cho consumer xử lý UserDeletedEvent.
 *
 * Bao gồm 3 loại queue:
 *
 * 1. MAIN QUEUE
 *    queue chính nhận event từ exchange
 *
 * 2. DLQ (Dead Letter Queue)
 *    chứa message bị lỗi sau khi retry quá số lần cho phép
 *
 * 3. RETRY QUEUE (delay queue)
 *    dùng để retry message sau 1 khoảng thời gian (TTL)
 *
 * Flow retry:
 *
 * main queue -> lỗi -> retry exchange
 * retry queue (delay TTL)
 * -> quay lại main queue -> xử lý lại
 *
 * nếu retry quá MAX_RETRY:
 *
 * main queue -> DLQ exchange -> DLQ queue
 */
@Configuration
public class RabbitConsumerConfig {

    // =========================
    // 1. MAIN QUEUE
    // =========================

    /*
     * Queue chính nhận event USER_DELETED
     *
     * durable:
     * queue sẽ được lưu trên disk của RabbitMQ
     * khi restart broker queue vẫn tồn tại
     *
     * x-dead-letter-exchange:
     * nếu message bị reject hoặc bị lỗi
     * message sẽ được gửi sang DLQ exchange
     *
     * x-dead-letter-routing-key:
     * routing key dùng khi gửi sang DLQ exchange
     */
    @Bean
    public Queue userQueue() {
        return QueueBuilder
                .durable(USER_DELETED_QUEUE)

                /*
                 * nếu message bị reject hoặc expire
                 * sẽ chuyển sang DLQ exchange
                 */
                .withArgument(
                        "x-dead-letter-exchange",
                        USER_DLQ_EXCHANGE
                )

                /*
                 * routing key khi chuyển sang DLQ
                 */
                .withArgument(
                        "x-dead-letter-routing-key",
                        USER_DELETED_DLQ_ROUTING
                )

                .build();
    }

    /*
     * Binding giữa queue chính và exchange chính
     */
    @Bean
    public Binding userBinding(Queue userQueue,
                               TopicExchange userExchange) {

        return BindingBuilder
                .bind(userQueue)
                .to(userExchange)
                .with(USER_DELETED_ROUTING);
    }

    // =========================
    // 2. DLQ QUEUE
    // =========================

    /*
     * DLQ = Dead Letter Queue
     * chứa message bị lỗi sau khi retry quá số lần cho phép.
     * dùng để:
     * - debug lỗi
     * - kiểm tra message lỗi
     * - xử lý thủ công
     * - gửi alert
     */
    @Bean
    public Queue dlqQueue() {
        return QueueBuilder
                .durable(USER_DELETED_DLQ_QUEUE)
                .build();
    }

    /*
     * binding DLQ queue với DLQ exchange
     */
    @Bean
    public Binding dlqBinding(Queue dlqQueue,
                              TopicExchange userDlqExchange) {

        return BindingBuilder
                .bind(dlqQueue)
                .to(userDlqExchange)
                .with(USER_DELETED_DLQ_ROUTING);
    }

    // =========================
    // 3. RETRY QUEUE (delay)
    // =========================

    /*
     * Retry queue dùng để delay trước khi retry.
     * cơ chế:
     * consumer fail
     * -> gửi message sang retry exchange
     * -> message vào retry queue
     * -> chờ TTL
     * -> RabbitMQ tự động chuyển message về exchange chính
     * -> consume lại
     * TTL = thời gian delay trước khi retry
     */
    @Bean
    public Queue retryQueue() {
        return QueueBuilder
                .durable(USER_DELETED_RETRY_QUEUE)

                /*
                 * sau khi TTL hết hạn
                 * message sẽ được gửi lại exchange chính
                 */
                .withArgument(
                        "x-dead-letter-exchange",
                        USER_EXCHANGE
                )

                /*
                 * routing key khi quay lại queue chính
                 */
                .withArgument(
                        "x-dead-letter-routing-key",
                        USER_DELETED_ROUTING
                )

                /*
                 * TTL = thời gian delay trước khi retry
                 * ví dụ:
                 * 5000 ms = 5 giây
                 * message sẽ chờ 5 giây rồi quay lại queue chính
                 */
                .withArgument(
                        "x-message-ttl",
                        RETRY_TTL_MS
                )

                .build();
    }

    /*
     * binding retry queue với retry exchange
     */
    @Bean
    public Binding retryBinding(Queue retryQueue,
                                TopicExchange userRetryExchange) {

        return BindingBuilder
                .bind(retryQueue)
                .to(userRetryExchange)
                .with(USER_DELETED_RETRY_ROUTING);
    }
}