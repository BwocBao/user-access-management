package com.r2s.user.mapper;


import com.r2s.user.dto.UserResponse;
import com.r2s.user.entity.UserProfile;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserResponse toUserResponse(UserProfile user) {
        return UserResponse.builder()
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }
}
