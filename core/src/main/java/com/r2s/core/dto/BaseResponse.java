package com.r2s.core.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
//@JsonInclude(JsonInclude.Include.NON_NULL) // trường nào null thì nó ko hiện
public class BaseResponse<T> {

    private int status;
    private String message;
    private T data;
    private LocalDateTime time;

    // ===== Factory methods =====

    public static <T> BaseResponse<T> success(T data, String message) {

        return BaseResponse.<T>builder()
                .status(200)
                .message(message)
                .data(data)
                .time(LocalDateTime.now())
                .build();
    }

    public static <T> BaseResponse<T> created(T data, String message) {

        return BaseResponse.<T>builder()
                .status(204)
                .message(message)
                .data(data)
                .time(LocalDateTime.now())
                .build();
    }

    public static <T> BaseResponse<T> error(int status, String message) {

        return BaseResponse.<T>builder()
                .status(status)
                .message(message)
                .time(LocalDateTime.now())
                .build();
    }


}
