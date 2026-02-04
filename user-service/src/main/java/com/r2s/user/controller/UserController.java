package com.r2s.user.controller;

import com.r2s.core.dto.ApiResponse;
import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.dto.UserResponse;
import com.r2s.user.service.UserService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping("/hello")
    public String hello() {
        return "Hello from User Service";
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers(),("Users retrieved successfully")));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getMyProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        String role = auth.getAuthorities()
                .iterator()
                .next()
                .getAuthority();

        return ResponseEntity.ok(ApiResponse
                .success(userService.getUserByUsername(username, role),"Profile retrieved successfully"));
    }

    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateMyProfile(@Valid @RequestBody UpdateUserRequest updateUserRequest,
                                                        Authentication auth) {
        String username = auth.getName();
        String role = auth.getAuthorities()
                .iterator()
                .next()
                .getAuthority();
        return ResponseEntity.ok(ApiResponse.success(userService.updateUser(updateUserRequest,username,role),"Profile updated successfully"));
    }

    @DeleteMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser( @PathVariable("username") String username) {
        userService.deleteUserByUsername(username);
        return ResponseEntity.noContent().build();
    }
}
