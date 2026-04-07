package com.r2s.core.messaging;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/*
 * Class cấu hình RabbitMQ dùng chung cho hệ thống.
 *
 * Nhiệm vụ chính:
 * 1. Tạo ConnectionFactory để kết nối tới RabbitMQ
 * 2. Tạo JSON converter để convert object <-> message
 * 3. Tạo RabbitTemplate để publish message
 * 4. Gắn callback để xử lý kết quả publish (ACK / NACK / RETURNED)
 */
@Configuration
@Slf4j
public class RabbitConfig {

    /*
     * Tạo ConnectionFactory kết nối tới RabbitMQ.
     * Các giá trị host, port, username, password lấy từ application.yml
     * @Value("${spring.rabbitmq.host}")       -> lấy host
     * @Value("${spring.rabbitmq.port:5672}")  -> lấy port, mặc định 5672 nếu không cấu hình
     * @Value("${spring.rabbitmq.username}")   -> lấy username
     * @Value("${spring.rabbitmq.password}")   -> lấy password
     */
    @Bean
    public CachingConnectionFactory connectionFactory(
            @Value("${spring.rabbitmq.host}") String host,
            @Value("${spring.rabbitmq.port:5672}") int port,
            @Value("${spring.rabbitmq.username}") String username,
            @Value("${spring.rabbitmq.password}") String password) {

        /*
         * CachingConnectionFactory: Spring wrapper cho RabbitMQ connection.
         * Nó giúp tái sử dụng connection/channel thay vì tạo mới liên tục, nhờ đó hiệu năng tốt hơn.
         */
        CachingConnectionFactory factory = new CachingConnectionFactory(host, port);
        factory.setUsername(username);
        factory.setPassword(password);

        /*
         * Publisher Confirm:
         * cho phép publisher biết broker đã nhận message hay chưa.
         * CORRELATED nghĩa là có hỗ trợ correlation data, giúp biết ACK/NACK này thuộc message nào.
         * Đây là phần rất quan trọng trong Outbox Pattern,
         * vì sau khi publish xong phải biết record outbox nào được mark SENT.
         */
        factory.setPublisherConfirmType(CachingConnectionFactory.ConfirmType.CORRELATED);

        /*
         * Publisher Returns:
         * bật cơ chế trả message về nếu message tới exchange nhưng không route được tới queue nào.
         * Ví dụ:
         * - exchange tồn tại
         * - nhưng routing key sai
         * -> message bị returned
         */
        factory.setPublisherReturns(true);

        return factory;
    }

    /*
     * Converter dùng Jackson để tự động convert:
     * - Java object -> JSON message khi gửi
     * - JSON message -> Java object khi nhận
     */
    @Bean
    public Jackson2JsonMessageConverter converter() {
        return new Jackson2JsonMessageConverter();
    }

    /*
     * Tạo RabbitTemplate để publish message.
     * RabbitTemplate giống như "client" để gửi message sang RabbitMQ.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(CachingConnectionFactory cf,
                                         Jackson2JsonMessageConverter converter,
                                         OutboxConfirmHandler outboxConfirmHandler) {

        RabbitTemplate template = new RabbitTemplate(cf);

        /*
         * Gắn JSON converter để tự động serialize object khi gửi.
         */
        template.setMessageConverter(converter);

        /*
         * mandatory = true:
         * nếu exchange nhận được message nhưng không route tới queue nào,
         * RabbitMQ sẽ trả message về qua ReturnsCallback.
         * Nếu không bật mandatory,
         * có thể message bị "rơi" mà publisher không biết.
         */
        template.setMandatory(true);

        /*
         * ConfirmCallback:
         * callback khi broker phản hồi ACK hoặc NACK cho message publisher gửi lên.
         * Nếu ACK:
         * broker đã nhận message
         * Nếu NACK:
         * broker không nhận được message
         * Ở đây chuyển xử lý sang OutboxConfirmHandler.
         */
        template.setConfirmCallback(outboxConfirmHandler::handleConfirm);

        /*
         * ReturnsCallback:
         * callback khi message đã tới exchange
         * nhưng exchange không route được message tới queue nào.
         * Ví dụ routing key sai.
         * Ở đây chuyển xử lý sang OutboxConfirmHandler.
         */
        template.setReturnsCallback(outboxConfirmHandler::handleReturned);

        return template;
    }

    @PostConstruct
    public void init() {
        log.info("RabbitConfig loaded");
    }
}