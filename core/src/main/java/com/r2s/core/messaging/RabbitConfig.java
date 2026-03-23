package com.r2s.core.messaging;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.r2s.core.messaging.RabbitConstants.*;

@Configuration
@Slf4j
public class RabbitConfig {

    @Bean
    public CachingConnectionFactory connectionFactory(
            @Value("${spring.rabbitmq.host}") String host,
            @Value("${spring.rabbitmq.username}") String username,
            @Value("${spring.rabbitmq.password}") String password) {

        CachingConnectionFactory factory = new CachingConnectionFactory(host);
        factory.setUsername(username);
        factory.setPassword(password);

        factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);
        factory.setPublisherReturns(true);

        return factory;
    }

    @Bean
    public Jackson2JsonMessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate retryRabbitTemplate(CachingConnectionFactory cf,
                                              Jackson2JsonMessageConverter converter) {

        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(converter);

        // ✅ QUAN TRỌNG: gắn callback cho retry template
        template.setConfirmCallback((cd, ack, cause) ->
                handleRetry(template, cd, ack, cause)
        );

        return template;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory cf,
                                         Jackson2JsonMessageConverter converter,
                                         RabbitTemplate retryRabbitTemplate) {

        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(converter);

        template.setMandatory(true);

        // ✅ callback chính
        template.setConfirmCallback((cd, ack, cause) ->
                handleRetry(retryRabbitTemplate, cd, ack, cause)
        );

        template.setReturnsCallback(returned -> {

            Message message = returned.getMessage();
            String messageId = message.getMessageProperties().getMessageId();

            log.error(
                    "Routing failed: messageId={}, exchange={}, routingKey={}, reason={}",
                    messageId,
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    returned.getReplyText()
            );
        });

        return template;
    }

    // =========================
    // 🔥 CORE RETRY LOGIC
    // =========================
    private void handleRetry(RabbitTemplate template,
                             CorrelationData correlationData,
                             boolean ack,
                             String cause) {

        if (ack) return;

        log.error("Publish failed: cause={}", cause);

        if (correlationData instanceof CustomCorrelationData cd) {

            Message message = cd.getMessage();
            if (message == null) return;

            String exchange = cd.getExchange();
            String routingKey = cd.getRoutingKey();

            Integer retry = (Integer) message.getMessageProperties()
                    .getHeaders()
                    .getOrDefault(HEADER_PUBLISH_RETRY_COUNT, 0);

            if (retry < MAX_RETRY) {

                int nextRetry = retry + 1;

                message.getMessageProperties()
                        .getHeaders()
                        .put(HEADER_PUBLISH_RETRY_COUNT, nextRetry);

                log.warn("Retry publish attempt {}", nextRetry);

                try {
                    // (optional) delay nhẹ tránh spam
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
//                template.send(USER_EXCHANGE, routingKey, message, correlationData);
                template.send(exchange, routingKey, message, correlationData);

            } else {

                log.error("Drop message after max retry. cause={}", cause);
            }
        }
    }

    @PostConstruct
    public void init() {
        log.info("RabbitConfig loaded");
    }
}