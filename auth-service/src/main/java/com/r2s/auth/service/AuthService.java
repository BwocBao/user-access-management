package com.r2s.auth.service;

import com.r2s.auth.client.UserServiceClient;
import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.dto.RegisterRequest;
import com.r2s.auth.dto.RegisterRoleRequest;
import com.r2s.auth.entity.User;
import com.r2s.auth.messaging.EventPublisher;
import com.r2s.core.entity.Role;
import com.r2s.core.exception.CustomException;
import com.r2s.core.exception.UnAuthorizedException;
import com.r2s.core.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.r2s.auth.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserServiceClient userServiceClient;
    private final EventPublisher  eventPublisher;

    @Transactional
    public void register(RegisterRequest registerRequest) {
        if(userRepository.findByUsername(registerRequest.getUsername()).isPresent()){
            throw new CustomException("Username exist");
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole(Role.ROLE_USER);
        userRepository.save(user);

        // 🔥 publish event thay vì HTTP
        eventPublisher.publishUserRegistered(user.getUsername());
//        eventPublisher.publishFakeUserRegistered(user.getUsername());

//        userServiceClient.syncUser(user.getUsername());
    }

    public AuthResponse login(LoginRequest loginRequest) {
        User user=userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new UnAuthorizedException("Username does not exist"));

        if(!passwordEncoder.matches(loginRequest.getPassword(),user.getPassword())){
            throw new UnAuthorizedException("Wrong password");
        }

        String accessToken = jwtUtil.generateToken(
                user.getUsername(),
                user.getRole().name()
        );
        return new AuthResponse(accessToken);

    }

    @Transactional
    public void registerRole(RegisterRoleRequest req) {
        if(userRepository.findByUsername(req.getUsername()).isPresent()){
            throw new CustomException("Username exist");
        }

        User user = new User();
        Role role;
        try {
            role = Role.valueOf(req.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException("Invalid role");
        }
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(role);
        userRepository.save(user);

        userServiceClient.syncUser(
                user.getUsername()
        );
    }
}
