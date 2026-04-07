package com.r2s.auth.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.core.entity.Outbox;
import com.r2s.core.messaging.event.UserRegisteredEvent;
import com.r2s.core.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.r2s.core.messaging.RabbitConstants.USER_EXCHANGE;
import static com.r2s.core.messaging.RabbitConstants.USER_REGISTERED_ROUTING;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository; // Repository thao tác bảng outbox
    private final ObjectMapper objectMapper; // Convert object Java <-> JSON

    public void saveUserRegisteredEvent(String username) {
        try {
            // Tạo event business/integration event khi user đăng ký thành công
            UserRegisteredEvent event = new UserRegisteredEvent(username);

            // Tạo record outbox để lưu event vào DB
            Outbox outbox = new Outbox();
            outbox.setId(UUID.randomUUID().toString()); // ID duy nhất cho record
            outbox.setExchange(USER_EXCHANGE); // Exchange đích
            outbox.setRoutingKey(USER_REGISTERED_ROUTING); // Routing key đích
            outbox.setPayload(objectMapper.writeValueAsString(event)); // Event -> JSON
            outbox.setStatus("PENDING"); // Ban đầu chưa gửi nên là PENDING
            outbox.setRetryCount(0); // Chưa retry lần nào
            outbox.setCreatedAt(LocalDateTime.now()); // Thời điểm tạo record

            // Lưu vào DB để tiến trình khác gửi sang RabbitMQ sau
            outboxRepository.save(outbox);

        } catch (Exception e) {
            // Nếu serialize JSON hoặc save DB lỗi thì ném exception
            throw new RuntimeException("Cannot save outbox", e);
        }
    }
}
