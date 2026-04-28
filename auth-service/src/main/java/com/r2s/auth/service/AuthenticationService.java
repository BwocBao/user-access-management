package com.r2s.auth.service;

import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;

public interface AuthenticationService {
    AuthResponse login(LoginRequest request);
}