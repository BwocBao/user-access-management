package com.r2s.auth.service;

import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;

public interface LoginService {
    AuthResponse login(LoginRequest request);
}