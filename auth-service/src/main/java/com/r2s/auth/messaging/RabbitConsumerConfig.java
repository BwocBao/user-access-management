package com.r2s.auth.messaging;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.r2s.core.messaging.RabbitConstants.*;
import static com.r2s.core.messaging.RabbitConstants.RETRY_TTL_MS;
import static com.r2s.core.messaging.RabbitConstants.USER_EXCHANGE;
import static com.r2s.core.messaging.RabbitConstants.USER_REGISTERED_DLQ_QUEUE;
import static com.r2s.core.messaging.RabbitConstants.USER_REGISTERED_DLQ_ROUTING;
import static com.r2s.core.messaging.RabbitConstants.USER_REGISTERED_RETRY_QUEUE;
import static com.r2s.core.messaging.RabbitConstants.USER_REGISTERED_RETRY_ROUTING;
import static com.r2s.core.messaging.RabbitConstants.USER_REGISTERED_ROUTING;

@Configuration
public class RabbitConsumerConfig {
    // =========================
    // 1. MAIN QUEUE(Exchange đã có ở core)
    // =========================
    @Bean
    public Queue userQueue() {
        return QueueBuilder
                .durable(USER_DELETED_QUEUE)
                .withArgument("x-dead-letter-exchange", USER_DLQ_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", USER_DELETED_DLQ_ROUTING)
                .build();
    }

    @Bean
    public Binding userBinding(Queue userQueue,
                               TopicExchange userExchange) {

        return BindingBuilder
                .bind(userQueue)
                .to(userExchange)
                .with(USER_DELETED_ROUTING);
    }

    // =========================
    // 2. DLQ QUEUE (Exchange đã có ở core)
    // =========================
    @Bean
    public Queue dlqQueue() {
        return QueueBuilder
                .durable(USER_DELETED_DLQ_QUEUE)
                .build();
    }

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
    @Bean
    public Queue retryQueue() {
        return QueueBuilder
                .durable(USER_DELETED_RETRY_QUEUE)
                .withArgument("x-dead-letter-exchange", USER_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", USER_DELETED_ROUTING)
                .withArgument("x-message-ttl", RETRY_TTL_MS)
                .build();
    }

    @Bean
    public Binding retryBinding(Queue retryQueue,
                                TopicExchange userRetryExchange) {

        return BindingBuilder
                .bind(retryQueue)
                .to(userRetryExchange)
                .with(USER_DELETED_RETRY_ROUTING);
    }
}