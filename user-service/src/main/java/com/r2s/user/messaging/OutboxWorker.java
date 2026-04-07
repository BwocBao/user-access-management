package com.r2s.user.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.core.entity.Outbox;
import com.r2s.core.messaging.CustomCorrelationData;
import com.r2s.core.messaging.event.UserDeletedEvent;
import com.r2s.core.repository.OutboxRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.r2s.core.messaging.RabbitConstants.MAX_RETRY;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxWorker {

    private final OutboxRepository outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 3000)
    public void process() {
        List<Outbox> list = outboxRepository.findTop10ByStatusOrderByCreatedAtAsc("PENDING");

        for (Outbox o : list) {

            if (o.getRetryCount() >= MAX_RETRY) {
                log.error("Outbox {} exceeded max retry -> FAILED", o.getId());
                outboxRepository.markAsFailed(o.getId());
                continue;
            }

            int locked = outboxRepository.markAsProcessing(o.getId());
            if (locked == 0) {
                log.debug("Skip outbox {} because it is no longer PENDING", o.getId());
                continue;
            }

            try {
                log.info("Publishing outboxId={}, retryCount={}", o.getId(), o.getRetryCount());

                UserDeletedEvent event =
                        objectMapper.readValue(o.getPayload(), UserDeletedEvent.class);

                CustomCorrelationData cd =
                        new CustomCorrelationData(o.getExchange(), o.getRoutingKey());

                cd.setOutboxId(o.getId());

                rabbitTemplate.convertAndSend(
                        o.getExchange(),
                        o.getRoutingKey(),
                        event,
                        msg -> {
                            msg.getMessageProperties().setMessageId(o.getId());
                            cd.setMessage(msg);
                            return msg;
                        },
                        cd
                );

            } catch (Exception e) {
                log.error("Local publish error, outboxId={}", o.getId(), e);

                int nextRetry = o.getRetryCount() + 1;
                if (nextRetry >= MAX_RETRY) {
                    outboxRepository.markAsFailed(o.getId());
                } else {
                    outboxRepository.increaseRetryAndRequeue(o.getId());
                }
            }
        }
    }

    @PostConstruct
    public void init() {
        log.warn("🔥 OutboxWorker running in USER SERVICE");
    }
}