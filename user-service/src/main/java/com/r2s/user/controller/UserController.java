package com.r2s.user.controller;


import com.r2s.core.dto.BaseResponse;
import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.dto.UserResponse;
import com.r2s.user.service.UserManagementService;
import com.r2s.user.service.UserProfileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(
        name = "User Management",
        description = "APIs for managing users and user profiles"
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserManagementService userManagementService;
    private final UserProfileService userProfileService;

    @Operation(
            summary = "Health check endpoint",
            description = "Simple endpoint to verify that User Service is running"
    )
    @GetMapping("/hello")
    public String hello() {
        return "Hello from User Service";
    }

    @Operation(
            summary = "Get all users",
            description = "Retrieve the list of all users. Only accessible by ADMIN role."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all users"),
            @ApiResponse(responseCode = "403", description = "Access denied - ADMIN role required"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<List<UserResponse>>> getAllUsers() {

        return ResponseEntity.ok(
                BaseResponse.success(
                        userManagementService.getAllUsers(),
                        "Users retrieved successfully"
                )
        );
    }

    @Operation(
            summary = "Get current user profile",
            description = "Retrieve profile information of the currently authenticated user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<BaseResponse<UserResponse>> getMyProfile(Authentication auth) {

        String username = auth.getName();

        return ResponseEntity.ok(
                BaseResponse.success(
                        userProfileService.getUser(username),
                        "Profile retrieved successfully"
                )
        );
    }

    @Operation(
            summary = "Update current user profile",
            description = "Update profile information of the currently authenticated user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request body"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    @PutMapping("/me")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ResponseEntity<BaseResponse<UserResponse>> updateMyProfile(
            @Valid @RequestBody UpdateUserRequest updateUserRequest) {

        return ResponseEntity.ok(
                BaseResponse.success(
                        userProfileService.updateUser(updateUserRequest),
                        "Profile updated successfully"
                )
        );
    }

    @Operation(
            summary = "Delete user by username",
            description = "Delete a user by username. Only ADMIN can perform this action."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser( @PathVariable("username") String username) {

        userManagementService.deleteUser(username);
        return ResponseEntity.noContent().build();
    }
}