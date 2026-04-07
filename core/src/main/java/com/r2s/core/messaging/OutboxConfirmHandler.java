package com.r2s.core.messaging;

import com.r2s.core.entity.Outbox;
import com.r2s.core.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static com.r2s.core.messaging.RabbitConstants.MAX_RETRY;

/*
 * Service xử lý callback sau khi publish message từ RabbitTemplate.
 * Đây là phần rất quan trọng trong Outbox Pattern:
 * - publish thành công  -> mark record outbox = SENT
 * - publish thất bại    -> tăng retryCount, requeue lại
 * - routing lỗi         -> tăng retryCount hoặc mark FAILED
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxConfirmHandler {

    private final OutboxRepository outboxRepository;

    /*
     * Callback khi RabbitMQ broker phản hồi ACK / NACK cho publisher.
     * @param correlationData: dữ liệu liên kết để biết callback này thuộc message nào
     * @param ack: true = broker nhận thành công, false = broker từ chối / lỗi
     * @param cause: lý do nếu bị NACK
     */
    @Transactional
    public void handleConfirm(CorrelationData correlationData, boolean ack, String cause) {

        /*
         * Chỉ xử lý nếu correlationData là CustomCorrelationData.
         * Lý do:
         * bạn đang cần lấy outboxId từ correlation data
         * để biết phải update record outbox nào.
         * Nếu không đúng kiểu thì bỏ qua.
         */
        if (!(correlationData instanceof CustomCorrelationData cd)) {
            return;
        }

        /*
         * Lấy outboxId từ correlationData.
         * Đây là id của record trong bảng outbox.
         */
        String outboxId = cd.getOutboxId();

        /*
         * Nếu không có outboxId thì không biết update record nào -> bỏ qua.
         */
        if (outboxId == null) {
            return;
        }

        /*
         * Nếu broker ACK:
         * nghĩa là broker đã nhận được message từ publisher.
         */
        if (ack) {

            /*
             * mark record outbox là SENT
             */
            int updated = outboxRepository.markAsSent(outboxId);

            if (updated > 0) {
                log.info("Publish success -> mark SENT, outboxId={}", outboxId);
            } else {
                /*
                 * updated = 0 có thể do:
                 * - record không tồn tại
                 * - record đã được xử lý trước đó
                 * - status không phù hợp để update
                 */
                log.warn("Publish ack received but row not updated (maybe already handled), outboxId={}", outboxId);
            }
            return;
        }

        /*
         * Nếu broker NACK:
         * nghĩa là publish không thành công ở mức broker.
         */
        log.error("Publish NACK, outboxId={}, cause={}", outboxId, cause);

        /*
         * Thử requeue + tăng retryCount.
         */
        requeueOrFail(outboxId, "NACK: " + cause);
    }

    /*
     * Callback khi message đã tới exchange
     * nhưng exchange không route được tới queue nào.
     * Trường hợp này khác với NACK:
     * - NACK: broker không nhận message thành công
     * - RETURNED: broker nhận rồi, exchange cũng nhận rồi,
     *             nhưng không route được tới queue
     */
    @Transactional
    public void handleReturned(ReturnedMessage returned) {

        /*
         * Lấy raw message bị trả về
         */
        Message message = returned.getMessage();

        /*
         * Ở đây bạn đang lấy outboxId từ messageId.
         * Nghĩa là khi publish bạn đã set messageId = outboxId.
         */
        String outboxId = message.getMessageProperties().getMessageId();

        log.error(
                "Routing failed -> returned message, outboxId={}, exchange={}, routingKey={}, reason={}",
                outboxId,
                returned.getExchange(),
                returned.getRoutingKey(),
                returned.getReplyText()
        );

        /*
         * Không có outboxId thì không biết update record nào.
         */
        if (outboxId == null) {
            return;
        }

        /*
         * Thử requeue.
         */
        requeueOrFail(outboxId, "RETURNED: " + returned.getReplyText());
    }

    /*
     * Xử lý chung cho cả NACK và RETURNED:
     * - nếu chưa vượt MAX_RETRY -> tăng retryCount, đưa lại về PENDING/REQUEUE
     * - nếu vượt MAX_RETRY      -> mark FAILED
     */
    private void requeueOrFail(String outboxId, String reason) {

        /*
         * Tìm record outbox trong DB.
         */
        Optional<Outbox> optional = outboxRepository.findById(outboxId);

        if (optional.isEmpty()) {
            log.warn("Outbox not found when handling failure, outboxId={}, reason={}", outboxId, reason);
            return;
        }

        Outbox outbox = optional.get();

        /*
         * Tính số retry tiếp theo.
         */
        int nextRetry = outbox.getRetryCount() + 1;

        /*
         * Nếu đã vượt quá số lần retry cho phép
         * -> đánh dấu FAILED
         */
        if (nextRetry >= MAX_RETRY) {
            outboxRepository.markAsFailed(outboxId);

            log.error("Outbox exceeded max retry -> FAILED, outboxId={}, retryCount={}, reason={}",
                    outboxId, nextRetry, reason);
        } else {

            /*
             * Nếu chưa vượt MAX_RETRY:
             * tăng retryCount và đưa record về trạng thái để publish lại sau.
             * Tên method increaseRetryAndRequeue cho thấy
             * có thể nó đang:
             * - retryCount = retryCount + 1
             * - status = PENDING
             */
            int updated = outboxRepository.increaseRetryAndRequeue(outboxId);

            if (updated > 0) {
                log.warn("Outbox requeued, outboxId={}, nextRetry={}, reason={}",
                        outboxId, nextRetry, reason);
            } else {
                log.warn("Outbox failure received but row not requeued (maybe already handled), outboxId={}", outboxId);
            }
        }
    }
}