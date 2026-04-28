package com.r2s.auth.service;

import com.r2s.auth.dto.RegisterRequest;
import com.r2s.auth.dto.RegisterRoleRequest;
import com.r2s.auth.entity.User;
import com.r2s.auth.factory.UserFactory;
import com.r2s.auth.messaging.OutboxService;
import com.r2s.auth.repository.UserRepository;
import com.r2s.auth.service.RegistrationService;
import com.r2s.core.entity.Role;
import com.r2s.core.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final UserRepository userRepository;
    private final UserFactory userFactory;
    private final OutboxService outboxService;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        registerUser(request.getUsername(), request.getPassword(), Role.ROLE_USER);
    }

    @Override
    @Transactional
    public void registerWithRole(RegisterRoleRequest request) {
        Role role = parseRole(request.getRole());
        registerUser(request.getUsername(), request.getPassword(), role);
    }

    private void registerUser(String username, String password, Role role) {
        validateUsernameNotExists(username);

        User user = userFactory.createUser(username, password, role);
        userRepository.save(user);

        outboxService.saveUserRegisteredEvent(user.getUsername());

        log.info("User registered successfully: username={}, role={}", username, role);
    }

    private void validateUsernameNotExists(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new CustomException("Username already exists");
        }
    }

    private Role parseRole(String roleValue) {
        try {
            return Role.valueOf(roleValue.toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new CustomException("Invalid role");
        }
    }
}