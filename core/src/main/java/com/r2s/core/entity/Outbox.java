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
    private String id;

    private String exchange;
    private String routingKey;
    @Column(columnDefinition = "TEXT")
    private String payload;

    private String status; // PENDING, SENT, FAILED
    private int retryCount;

    private LocalDateTime createdAt;
}