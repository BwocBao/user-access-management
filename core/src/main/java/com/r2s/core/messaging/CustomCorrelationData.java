package com.r2s.core.messaging;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CustomCorrelationData extends CorrelationData {
    private String exchange;
    private String routingKey;
    private Message message;
    private String outboxId; // 🔥 thêm

    public CustomCorrelationData(String userExchange, String userRegistered) {
        this.exchange=userExchange;
        this.routingKey=userRegistered;
    }
}
// CustomCorrelationData dùng để lưu exchange, routingKey và message gốc
// vì Spring AMQP không cung cấp lại các thông tin này trong ConfirmCallback.
// Điều này cho phép implement cơ chế retry publish khi broker không xác nhận (ack=false).
//Producer
//   ↓
//convertAndSend()
//   ↓
//RabbitMQ
//   ↓
//ConfirmCallback (ack = false)
//   ↓
//lấy lại message + exchange + routingKey từ CustomCorrelationData
//   ↓
//retry publish