package com.r2s.core.messaging;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
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

        // Publisher Confirm
        factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);

        // Publisher Returns
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
        return template;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory cf,
                                         Jackson2JsonMessageConverter converter,RabbitTemplate retryRabbitTemplate) {

        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(converter);

        // bắt buộc để trigger ReturnsCallback
        template.setMandatory(true);

        // =========================
        // 1. CONFIRM CALLBACK (publisher -> exchange)
        // =========================
        template.setConfirmCallback((correlationData, ack, cause) -> {

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

                    message.getMessageProperties()
                            .getHeaders()
                            .put(HEADER_PUBLISH_RETRY_COUNT, retry + 1);

                    log.error("Retry publish attempt {}", retry + 1);

                    // ✅ dùng template KHÁC
                    retryRabbitTemplate.send(exchange, routingKey, message);

                } else {

                    log.error("Drop message after max retry. cause={}", cause);

                }
            }
        });

        // =========================
        // 2. RETURNS CALLBACK (exchange -> queue)
        // =========================
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

            // ❗ không retry vì lỗi config (routing sai / queue chưa bind)
        });

        return template;
    }

    @PostConstruct
    public void init() {
        log.info("RabbitConfig loaded");
    }
}