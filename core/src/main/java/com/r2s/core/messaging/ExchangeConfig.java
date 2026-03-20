package com.r2s.core.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.r2s.core.messaging.RabbitConstants.*;

@Configuration
public class ExchangeConfig {

    // =========================
    // MAIN EXCHANGE
    // =========================
    @Bean
    public TopicExchange userExchange() {
        return new TopicExchange(USER_EXCHANGE, true, false);
    }

    // =========================
    // RETRY EXCHANGE
    // =========================
    @Bean
    public TopicExchange userRetryExchange() {
        return new TopicExchange(USER_RETRY_EXCHANGE, true, false);
    }

    // =========================
    // DLQ EXCHANGE
    // =========================
    @Bean
    public TopicExchange userDlqExchange() {
        return new TopicExchange(USER_DLQ_EXCHANGE, true, false);
    }
}