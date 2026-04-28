package com.r2s.auth.controller;

import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.dto.RegisterRequest;
import com.r2s.auth.dto.RegisterRoleRequest;
import com.r2s.auth.service.AuthenticationService;
import com.r2s.auth.service.RegistrationService;
import com.r2s.core.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final RegistrationService registrationService;

    @GetMapping("/hello")
    public ResponseEntity<ApiResponse<String>> hello() {
        log.info("Health check from AuthController");
        return ResponseEntity.ok(
                ApiResponse.success("Hello from Auth Service", "Auth service is running")
        );
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Void>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        registrationService.register(request);

        return ResponseEntity.ok(
                ApiResponse.success(null, "User registered successfully")
        );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse response = authenticationService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success(response, "User logged in successfully")
        );
    }

    @PostMapping("/register/role")
    public ResponseEntity<ApiResponse<Void>> registerRole(
            @Valid @RequestBody RegisterRoleRequest request
    ) {
        registrationService.registerWithRole(request);

        return ResponseEntity.ok(
                ApiResponse.success(null, "User registered successfully")
        );
    }
}