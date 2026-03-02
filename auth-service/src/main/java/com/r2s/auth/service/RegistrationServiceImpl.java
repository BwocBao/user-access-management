package com.r2s.auth.service;

import com.r2s.auth.dto.RegisterRequest;
import com.r2s.auth.dto.RegisterRoleRequest;
import com.r2s.core.entity.Role;
import com.r2s.core.entity.User;
import com.r2s.core.exception.CustomException;
import com.r2s.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RegistrationServiceImpl implements RegistrationService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;

    @Override
    public void register(RegisterRequest request) {
        validateUsername(request.getUsername());

        User user = buildUser(
                request.getUsername(),
                request.getPassword(),
                Role.ROLE_USER
        );

        userRepository.save(user);
    }

    @Override
    public void registerWithRole(RegisterRoleRequest request) {
        validateUsername(request.getUsername());

        Role role = parseRole(request.getRole());

        User user = buildUser(
                request.getUsername(),
                request.getPassword(),
                role
        );

        userRepository.save(user);
    }

    private void validateUsername(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new CustomException("Username already exists");
        }
    }

    private Role parseRole(String roleStr) {
        try {
            return Role.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException("Invalid role");
        }
    }

    private User buildUser(String username, String password, Role role) {
        return User.builder()
                .username(username)
                .password(passwordService.encode(password))
                .role(role)
                .build();
    }
}