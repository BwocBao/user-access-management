package com.r2s.auth.strategy;

import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.entity.AuthType;

public interface LoginStrategy {
    AuthResponse authenticate(LoginRequest request);
    boolean supports(AuthType type);
}
