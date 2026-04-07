package com.r2s.core.messaging;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static com.r2s.core.messaging.RabbitConstants.USER_DLQ_EXCHANGE;
import static com.r2s.core.messaging.RabbitConstants.USER_EXCHANGE;
import static com.r2s.core.messaging.RabbitConstants.USER_RETRY_EXCHANGE;
import static org.junit.jupiter.api.Assertions.*;

class ExchangeConfigTest {

    @Test
    void userExchange_shouldBeCreatedCorrectly() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(ExchangeConfig.class)) {

            TopicExchange exchange = context.getBean("userExchange", TopicExchange.class);

            assertNotNull(exchange);
            assertEquals(USER_EXCHANGE, exchange.getName());
            assertTrue(exchange.isDurable());
            assertFalse(exchange.isAutoDelete());
        }
    }

    @Test
    void userRetryExchange_shouldBeCreatedCorrectly() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(ExchangeConfig.class)) {

            TopicExchange exchange = context.getBean("userRetryExchange", TopicExchange.class);

            assertNotNull(exchange);
            assertEquals(USER_RETRY_EXCHANGE, exchange.getName());
            assertTrue(exchange.isDurable());
            assertFalse(exchange.isAutoDelete());
        }
    }

    @Test
    void userDlqExchange_shouldBeCreatedCorrectly() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(ExchangeConfig.class)) {

            TopicExchange exchange = context.getBean("userDlqExchange", TopicExchange.class);

            assertNotNull(exchange);
            assertEquals(USER_DLQ_EXCHANGE, exchange.getName());
            assertTrue(exchange.isDurable());
            assertFalse(exchange.isAutoDelete());
        }
    }

    @Test
    void allExchanges_shouldExistInSpringContext() {
        try (AnnotationConfigApplicationContext context =
                     new AnnotationConfigApplicationContext(ExchangeConfig.class)) {

            assertTrue(context.containsBean("userExchange"));
            assertTrue(context.containsBean("userRetryExchange"));
            assertTrue(context.containsBean("userDlqExchange"));

            assertEquals(3, context.getBeansOfType(TopicExchange.class).size());
        }
    }
}