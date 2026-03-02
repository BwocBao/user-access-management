package com.r2s.auth.strategy;

import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.entity.AuthType;
import com.r2s.auth.service.PasswordService;
import com.r2s.core.entity.User;
import com.r2s.core.exception.UnAuthorizedException;
import com.r2s.core.repository.UserRepository;
import com.r2s.core.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UsernamePasswordAuthenticationStrategy implements LoginStrategy {

    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final JwtUtil jwtUtil;

    @Override
    public AuthResponse authenticate(LoginRequest request) {

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() ->
                        new UnAuthorizedException("Username not found"));

        if (!passwordService.matches(request.getPassword(), user.getPassword())) {
            throw new UnAuthorizedException("Wrong password");
        }

        String token = jwtUtil.generateToken(
                user.getUsername(),
                user.getRole().name()
        );

        return new AuthResponse(token);
    }

    @Override
    public boolean supports(AuthType type) {
        return AuthType.USERNAME_PASSWORD.equals(type);
    }
}