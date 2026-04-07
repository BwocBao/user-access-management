package com.r2s.auth.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.core.entity.Outbox;
import com.r2s.core.messaging.CustomCorrelationData;
import com.r2s.core.messaging.event.UserRegisteredEvent;
import com.r2s.core.repository.OutboxRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.r2s.core.messaging.RabbitConstants.MAX_RETRY;

/*
 * Worker chịu trách nhiệm đọc bảng OUTBOX
 * và publish message sang RabbitMQ.
 * Đây là phần quan trọng của Outbox Pattern:
 * business transaction chỉ lưu event vào DB
 * worker này sẽ publish event ra message broker sau.
 * đảm bảo:
 * - không mất message khi broker bị lỗi
 * - retry được khi publish fail
 * - eventual consistency giữa services
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxWorker {

    private final OutboxRepository outboxRepository;

    private final RabbitTemplate rabbitTemplate;

    private final ObjectMapper objectMapper;

    /*
     * @Scheduled:
     * method này chạy định kỳ mỗi 3 giây.
     *
     * fixedDelay = 3000 nghĩa là:
     * sau khi chạy xong sẽ chờ 3 giây rồi chạy lại.
     *
     * worker sẽ liên tục kiểm tra xem có event nào chưa publish không.
     */
    @Scheduled(fixedDelay = 3000)
    public void process() {

        /*
         * lấy tối đa 10 record outbox có status = PENDING
         * sắp xếp theo createdAt tăng dần (event cũ gửi trước)
         * giúp:
         * - không load quá nhiều record cùng lúc
         * - publish theo thứ tự thời gian
         */
        List<Outbox> list =
                outboxRepository.findTop10ByStatusOrderByCreatedAtAsc("PENDING");

        for (Outbox o : list) {

            /*
             * nếu retry vượt quá số lần cho phép
             * thì đánh dấu FAILED và bỏ qua
             */
            if (o.getRetryCount() >= MAX_RETRY) {

                log.error(
                        "Outbox {} exceeded max retry -> FAILED",
                        o.getId()
                );

                outboxRepository.markAsFailed(o.getId());

                continue;
            }

            /*
             * lock record để tránh nhiều worker xử lý cùng lúc.
             * markAsProcessing có thể update:
             * status = PROCESSING
             * where id = ? and status = PENDING
             * nếu update thành công -> return 1
             * nếu record đã bị worker khác xử lý -> return 0
             */
            int locked = outboxRepository.markAsProcessing(o.getId());

            if (locked == 0) {

                log.debug(
                        "Skip outbox {} because it is no longer PENDING",
                        o.getId()
                );

                continue;
            }

            try {

                log.info(
                        "Publishing outboxId={}, retryCount={}",
                        o.getId(),
                        o.getRetryCount()
                );

                /*
                 * convert payload JSON thành event object
                 * payload trong DB dạng:
                 * {"username":"haruki"}
                 */
                UserRegisteredEvent event =
                        objectMapper.readValue(
                                o.getPayload(),
                                UserRegisteredEvent.class
                        );

                /*
                 * CustomCorrelationData dùng để liên kết
                 * message publish với record outbox.
                 * khi RabbitMQ trả ACK/NACK,
                 * ConfirmHandler sẽ biết phải update record nào.
                 */
                CustomCorrelationData cd =
                        new CustomCorrelationData(
                                o.getExchange(),
                                o.getRoutingKey()
                        );

                /*
                 * lưu outboxId vào correlation data
                 * để callback biết record nào cần update status.
                 */
                cd.setOutboxId(o.getId());

                /*
                 * publish message sang RabbitMQ
                 * convertAndSend:
                 * 1. serialize event -> JSON
                 * 2. gửi message tới exchange + routing key
                 * msg -> callback để chỉnh message trước khi gửi
                 * correlationData -> dùng trong confirm callback
                 */
                rabbitTemplate.convertAndSend(
                        o.getExchange(),
                        o.getRoutingKey(),
                        event,
                        /*
                         * chỉnh message trước khi gửi
                         */
                        msg -> {
                            /*
                             * set messageId = outboxId
                             * giúp:
                             * - trace log
                             * - idempotency consumer
                             * - xác định message khi bị returned
                             */
                            msg.getMessageProperties()
                                    .setMessageId(o.getId());

                            /*
                             * lưu message vào correlation data
                             * để callback có thể dùng lại.
                             */
                            cd.setMessage(msg);

                            return msg;
                        },

                        /*
                         * correlationData giúp callback biết
                         * message này thuộc record outbox nào.
                         */
                        cd
                );

            } catch (Exception e) {

                /*
                 * lỗi xảy ra ngay tại local khi publish:
                 *
                 * ví dụ:
                 * - JSON parse lỗi
                 * - connection RabbitMQ lỗi
                 * - network lỗi
                 */
                log.error(
                        "Local publish error, outboxId={}",
                        o.getId(),
                        e
                );

                int nextRetry = o.getRetryCount() + 1;

                /*
                 * nếu retry quá số lần cho phép -> FAILED
                 */
                if (nextRetry >= MAX_RETRY) {

                    outboxRepository.markAsFailed(o.getId());

                } else {

                    /*
                     * tăng retryCount
                     * đưa status về PENDING để worker xử lý lại sau
                     */
                    outboxRepository.increaseRetryAndRequeue(o.getId());
                }
            }
        }
    }

    /*
     * log khi worker được khởi động
     * giúp kiểm tra worker có đang chạy hay không.
     */
    @PostConstruct
    public void init() {

        log.warn("🔥 OutboxWorker running in AUTH SERVICE");

    }
}