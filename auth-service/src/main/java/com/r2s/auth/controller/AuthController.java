package com.r2s.auth.controller;

import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.dto.RegisterRequest;
import com.r2s.auth.dto.RegisterRoleRequest;
import com.r2s.auth.service.LoginService;
import com.r2s.auth.service.RegistrationService;
import com.r2s.core.dto.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@Tag(
        name = "Authentication",
        description = "APIs for authentication and registration"
)
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginService loginService;
    private final RegistrationService registrationService;

    @Operation(
            summary = "Health check endpoint",
            description = "Simple endpoint to verify that Auth Service is running"
    )
    @GetMapping("/hello")
    public String hello() {
        return "Hello from Auth Service";
    }

    @Operation(
            summary = "User login",
            description = "Authenticate user and return JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<BaseResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(
                BaseResponse.success(
                        loginService.login(request),
                        "Login success"
                )
        );
    }

    @Operation(
            summary = "Register new user",
            description = "Create a new user account with default role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Register successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
    @PostMapping("/register")
    public ResponseEntity<BaseResponse<Void>> register(
            @Valid @RequestBody RegisterRequest request) {

        registrationService.register(request);

        return ResponseEntity.ok(
                BaseResponse.success(null, "Register success")
        );
    }


    @Operation(
            summary = "Register user with specific role",
            description = "Create a new user account and assign a specific role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Register successful"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PostMapping("/register/role")
    public ResponseEntity<BaseResponse> registerRole(@Valid @RequestBody RegisterRoleRequest registerRoleRequest) {
        registrationService.registerWithRole(registerRoleRequest);
        return ResponseEntity.ok(
                BaseResponse.success(null,"Register success"));
    }
}

//@RestController
//@RequestMapping("/api/auth")
//@RequiredArgsConstructor
//public class AuthController {
//    private final AuthService authService;
//    @GetMapping("/hello")
//    public String hello() {
//        return "Hello from Auth Service";
//    }
//
//    @PostMapping("/register")
//    public ResponseEntity<BaseResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
//        authService.register(registerRequest);
//        return ResponseEntity.ok(BaseResponse.success(null,"User registered successfully"));
//    }
//
//    @PostMapping("/login")
//    public ResponseEntity<BaseResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
//        return ResponseEntity.ok(BaseResponse.success(authService.login(loginRequest),"User logged successfully") );
//    }
//
//    @PostMapping("/register/role")
//    public ResponseEntity<BaseResponse> registerRole(@Valid @RequestBody RegisterRoleRequest registerRoleRequest) {
//        authService.registerRole(registerRoleRequest);
//        return ResponseEntity.ok(BaseResponse.success(null,"User registered successfully"));
//    }
//}
