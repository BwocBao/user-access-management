# Đối chiếu triển khai với 4.5 Phương án E (Event-driven RabbitMQ)

**Tham chiếu:** `docs/auth-user-sync-analysis.md` — mục **4.5 Phương án E: Event-driven (Kafka/RabbitMQ)**.

**Mục đích tài liệu này:** Tóm tắt kết luận đối chiếu source với tài liệu, và bảng hướng dẫn Fresher chỉnh sửa / hoàn thiện khi cần.

---

## 1. Tóm tắt kết luận

- **Phần cốt lõi của 4.5 đã được triển khai:** auth-service publish event **`UserRegistered`** sau khi đăng ký thành công; user-service **subscribe** (RabbitMQ consumer) và **tạo profile** tương ứng. Broker dùng **RabbitMQ** — khớp mục “Kafka/RabbitMQ” trong tài liệu (message queue, không bắt buộc đúng tên Kafka).
- **Ưu điểm nêu trong 4.5** (tách service, retry, mở rộng) có phần hiện diện trong code: publisher confirm + retry header, consumer retry/DLQ, xử lý trùng (idempotent + bắt `DataIntegrityViolationException`).
- **Eventual consistency** (nhược điểm ghi trong doc) vẫn tồn tại: user-service còn **lazy create** profile khi gọi API mà chưa nhận được message — đây là hành vi phù hợp mô hình eventual consistency, không phải “sai” so với 4.5.
- **Không thuần một mình 4.5:** `registerRole` dùng **HTTP sync** (gần phương án A), có thêm luồng **`UserDeleted`** event-driven; `docker-compose` hiện **không** gói RabbitMQ.

---

## 2. Bảng đối chiếu: Yêu cầu 4.5 ↔ Code

| # | Nội dung 4.5 / tài liệu | Trạng thái | Ghi chú ngắn |
|---|------------------------|------------|--------------|
| 1 | Auth publish `UserRegistered` sau khi tạo user | Đạt | `AuthService.register()` → `EventPublisher.publishUserRegistered()` |
| 2 | User-service subscribe và tạo profile | Đạt | `EventConsumer` + `@RabbitListener` queue `user.registered.queue` |
| 3 | Dùng message broker (Kafka hoặc tương đương) | Đạt | RabbitMQ (`spring.rabbitmq.*`, `RabbitTemplate`) |
| 4 | Tách biệt service, có thể retry | Một phần–đạt | Retry publish + consumer retry/DLQ; xem mục “Cần lưu ý” |
| 5 | Payload event chỉ cần đủ để tạo profile | Đạt | `UserRegisteredEvent` có `username`; `UserProfile` không có `role` trong entity hiện tại |

---

## 3. Bảng lệch / mở rộng so với đoạn 4.5 thuần túy

| # | Hiện tượng | Mức độ | Giải thích |
|---|------------|--------|------------|
| 1 | `registerRole` gọi HTTP `userServiceClient.syncUser()`, không publish RabbitMQ | Trung bình | Trộn phương án A (sync HTTP) với E; đồng nhất thì nên chọn một hướng (event hoặc HTTP) |
| 2 | Luồng `UserDeleted`: user-service publish → auth-service xóa user | Thông tin | Không nằm trong 4.5; bổ sung đồng bộ xóa theo hướng event-driven |
| 3 | `docker-compose` không có service RabbitMQ | Cao (môi trường) | App cấu hình `localhost:5672`; chạy full stack trong Docker cần broker hoặc env trỏ đúng host |
| 4 | `UserRegisteredEvent` không mang `role` | Thấp–trung bình | Khớp schema `UserProfile` hiện tại; nếu sau này profile cần role thì mở rộng event + DB |
| 5 | Lazy create khi GET profile | Thông tin | Hợp lý với eventual consistency; giảm phụ thuộc nếu consumer luôn kịp |

---

## 4. Bảng hướng dẫn: Việc nên làm khi chỉnh sửa

| Ưu tiên | Việc cần làm | Gợi ý thao tác / file liên quan |
|---------|--------------|----------------------------------|
| P0 | Đảm bảo RabbitMQ có khi chạy Docker / CI | Thêm service `rabbitmq` trong `docker-compose.yml`; set `SPRING_RABBITMQ_HOST`, `PORT`, user/pass cho `auth-service` và `user-service` |
| P1 | Thống nhất `registerRole` với `register` | Hoặc publish `UserRegistered` (kèm role nếu cần) và bỏ HTTP sync; hoặc ghi rõ trong doc là cố ý dùng HTTP cho admin flow |
| P2 | Ghi rõ contract event | Cập nhật `UserRegisteredEvent` + README/doc nếu thêm field (ví dụ `role`) |
| P2 | Review lazy create | Giữ nếu chấp nhận eventual consistency; hoặc chỉ tạo profile từ consumer + trả 404 cho đến khi có bản ghi (chặt hơn, UX khác) |
| P3 | Test tích hợp | Test register → message → profile tồn tại; test retry/DLQ |
| P3 | Quan sát thứ tự xóa | Hiện: xóa DB user-service trước, rồi publish `UserDeleted`; auth xóa sau — nắm rủi ro eventual consistency khi publish fail |

---

## 5. Checklist nhanh cho Fresher (trước khi merge / demo)

- [ ] RabbitMQ chạy và hai service kết nối được (log không lỗi connection).
- [ ] Đăng ký user mới → trong DB user-service có profile (sau vài giây nếu có độ trễ).
- [ ] Hiểu sự khác biệt giữa `register` (event) và `registerRole` (HTTP) trong code.
- [ ] Đọc thêm mục 5–6 trong `auth-user-sync-analysis.md` về thứ tự khuyến nghị (C, A, D, B) nếu làm tiếp delete/sync/verify.

---

## 6. Tham chiếu file chính (để đọc code)

| Vai trò | File gợi ý |
|---------|-------------|
| Publish đăng ký | `auth-service/.../AuthService.java`, `auth-service/.../messaging/EventPublisher.java` |
| Consume tạo profile | `user-service/.../messaging/EventConsumer.java` |
| Hằng số queue/routing | `core/.../messaging/RabbitConstants.java` |
| Cấu hình exchange/queue | `core/.../messaging/ExchangeConfig.java`, `user-service/.../messaging/RabbitConsumerConfig.java`, `auth-service/.../messaging/RabbitConsumerConfig.java` |
| Event xóa (mở rộng) | `user-service/.../messaging/EventPublisher.java`, `auth-service/.../messaging/EventConsumer.java` (UserDeleted) |

---

*Tài liệu được tổng hợp để onboard Fresher; cập nhật khi kiến trúc hoặc contract event thay đổi.*
