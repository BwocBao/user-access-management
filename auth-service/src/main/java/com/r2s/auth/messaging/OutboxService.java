package com.r2s.auth.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.core.repository.OutboxRepository;
import com.r2s.core.entity.Outbox;
import com.r2s.core.messaging.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.r2s.core.messaging.RabbitConstants.USER_EXCHANGE;
import static com.r2s.core.messaging.RabbitConstants.USER_REGISTERED_ROUTING;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public void saveUserRegisteredEvent(String username) {

        try {
            UserRegisteredEvent event = new UserRegisteredEvent(username);

            Outbox outbox = new Outbox();
            outbox.setId(UUID.randomUUID().toString());
            outbox.setExchange(USER_EXCHANGE);
//            outbox.setExchange("fake.exchange");
            outbox.setRoutingKey(USER_REGISTERED_ROUTING);
            outbox.setPayload(objectMapper.writeValueAsString(event));
            outbox.setStatus("PENDING");
            outbox.setRetryCount(0);
            outbox.setCreatedAt(LocalDateTime.now());

            outboxRepository.save(outbox);

        } catch (Exception e) {
            throw new RuntimeException("Cannot save outbox", e);
        }
    }
}