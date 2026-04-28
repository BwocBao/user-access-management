package com.r2s.auth.factory;

import com.r2s.auth.entity.User;
import com.r2s.auth.service.PasswordService;
import com.r2s.core.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserFactory {

    private final PasswordService passwordService;

    public User createUser(String username, String password, Role role) {
        return User.builder()
                .username(username)
                .password(passwordService.encode(password))
                .role(role)
                .build();
    }
}