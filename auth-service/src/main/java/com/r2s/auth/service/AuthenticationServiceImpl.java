package com.r2s.auth.service;


import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.entity.User;
import com.r2s.auth.repository.UserRepository;
import com.r2s.auth.service.AuthenticationService;
import com.r2s.auth.service.PasswordService;
import com.r2s.core.exception.UnAuthorizedException;
import com.r2s.core.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final JwtUtil jwtUtil;

    @Override
    public AuthResponse login(LoginRequest request) {
        log.info("Login attempt for username={}", request.getUsername());

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new UnAuthorizedException("Invalid username or password"));

        if (!passwordService.matches(request.getPassword(), user.getPassword())) {
            throw new UnAuthorizedException("Invalid username or password");
        }

        String token = jwtUtil.generateToken(
                user.getUsername(),
                user.getRole().name()
        );

        return AuthResponse.builder()
                .token(token)
                .build();
    }
}