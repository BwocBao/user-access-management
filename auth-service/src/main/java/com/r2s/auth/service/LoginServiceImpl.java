package com.r2s.auth.service;

import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.strategy.LoginStrategy;
import com.r2s.core.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private final List<LoginStrategy> strategies;

    @Override
    public AuthResponse login(LoginRequest request) {

        LoginStrategy strategy = strategies.stream()
                .filter(s -> s.supports(request.getType()))
                .findFirst()
                .orElseThrow(() ->
                        new CustomException("Authentication type not supported"));

        return strategy.authenticate(request);
    }
}