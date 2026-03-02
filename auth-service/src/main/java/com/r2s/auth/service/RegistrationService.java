package com.r2s.auth.service;

import com.r2s.auth.dto.RegisterRequest;
import com.r2s.auth.dto.RegisterRoleRequest;

public interface RegistrationService {
    void register(RegisterRequest request);
    void registerWithRole(RegisterRoleRequest request);
}