package com.r2s.user.mapper;


import com.r2s.core.entity.User;
import com.r2s.user.dto.UserResponse;

public class UserMapper {
    public static UserResponse toUserResponse(User user) {
        return UserResponse.builder().Role(user.getRole().toString())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }
}
