package com.r2s.core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Outbox {

    @Id
    private String id; // ID duy nhất của record outbox, thường dùng UUID

    private String exchange; // Tên RabbitMQ exchange sẽ publish message tới

    private String routingKey; // Routing key dùng để định tuyến message tới queue phù hợp

    @Column(columnDefinition = "TEXT")
    private String payload; // Nội dung message dưới dạng JSON

    private String status; // Trạng thái message: PENDING, PROCESSING, SENT, FAILED

    private int retryCount; // Số lần retry publish nếu gửi thất bại

    private LocalDateTime createdAt; // Thời điểm tạo record outbox
}