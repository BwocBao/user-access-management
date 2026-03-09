package com.r2s.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // trường nào null thì nó ko hiện
public class ApiResponse<T> {

    private int status;
    private String message;
    private T data;
    private LocalDateTime time;

    // ===== Factory methods =====

    public static <T> ApiResponse<T> success(T data, String message) {

        return ApiResponse.<T>builder()
                .status(200)
                .message(message)
                .data(data)
                .time(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> created(T data, String message) {

        return ApiResponse.<T>builder()
                .status(201)
                .message(message)
                .data(data)
                .time(LocalDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(int status, String message, T data) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .data(data)
                .time(LocalDateTime.now())
                .build();
    }


}
