package com.r2s.core.messaging;

import com.r2s.core.repository.OutboxRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.r2s.core.messaging.RabbitConstants.*;

@Configuration
@Slf4j
public class RabbitConfig {

    @Autowired
    private OutboxRepository outboxRepository;

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
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory cf,
                                         Jackson2JsonMessageConverter converter) {

        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(converter);

        template.setMandatory(true);

        // =========================
        // ✅ CONFIRM CALLBACK (chuẩn outbox)
        // =========================
        template.setConfirmCallback(this::handleConfirm);

        // =========================
        // ✅ RETURNS CALLBACK
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

            // ❗ KHÔNG retry
            // vì lỗi config → phải fix exchange/queue
        });

        return template;
    }

    // =========================
    // 🔥 CORE LOGIC (OUTBOX)
    // =========================
    private void handleConfirm(CorrelationData correlationData,
                               boolean ack,
                               String cause) {

        if (!(correlationData instanceof CustomCorrelationData cd)) return;

        log.warn("CONFIRM: ack={}, outboxId={}", ack, cd.getOutboxId());

        String outboxId = cd.getOutboxId();

        if (ack) {
            log.info("Publish success: {}", outboxId);
            outboxRepository.markAsSent(outboxId);
            return;
        }

        log.error("Publish failed: {}, cause={}", outboxId, cause);

        // ❌ KHÔNG retry ở đây
        // ✅ chỉ update DB
        outboxRepository.increaseRetry(outboxId);
    }

    @PostConstruct
    public void init() {
        log.info("RabbitConfig loaded");
    }
}