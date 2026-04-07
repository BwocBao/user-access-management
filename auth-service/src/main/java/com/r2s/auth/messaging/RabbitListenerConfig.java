package com.r2s.auth.messaging;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitListenerConfig {

    /*
     * Tạo container factory cho @RabbitListener
     * Container này quản lý:
     * - kết nối tới RabbitMQ
     * - số thread consumer
     * - cơ chế acknowledge
     * - cách convert message
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            Jackson2JsonMessageConverter converter) {

        SimpleRabbitListenerContainerFactory factory =
                new SimpleRabbitListenerContainerFactory();

        /*
         * ConnectionFactory chứa thông tin kết nối RabbitMQ:
         * host, port, username, password
         */
        factory.setConnectionFactory(connectionFactory);

        /*
         * Converter giúp tự động convert JSON message
         * thành object Java (VD: UserDeletedEvent)
         */
        factory.setMessageConverter(converter);

        /*
         * Số consumer chạy song song ngay khi start app.
         * Tức là có 3 thread cùng consume queue.
         */
        factory.setConcurrentConsumers(3);

        /*
         * Khi hệ thống tải cao, có thể scale tối đa 5 consumer.
         * Giúp tăng throughput khi có nhiều message.
         */
        factory.setMaxConcurrentConsumers(5);

        /*
         * Prefetch = số message RabbitMQ gửi trước cho mỗi consumer.
         * prefetch = 1 nghĩa là:
         * consumer xử lý xong message hiện tại thì RabbitMQ mới gửi message tiếp theo.
         * Ưu điểm:
         * - tránh 1 consumer giữ quá nhiều message
         * - tránh mất message khi consumer crash
         * - đảm bảo xử lý lần lượt từng message
         */
        factory.setPrefetchCount(1);

        /*
         * MANUAL ACK:
         * chúng ta tự quyết định khi nào message được xem là xử lý xong.
         * nếu không set manual:
         * Spring có thể auto ack trước khi xử lý xong -> dễ mất message.
         */
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);

        return factory;
    }
}