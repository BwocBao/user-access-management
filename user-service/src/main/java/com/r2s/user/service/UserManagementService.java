package com.r2s.user.service;

import com.r2s.user.dto.UserRequest;
import com.r2s.user.dto.UserResponse;

import java.util.List;

public interface UserManagementService {
    UserResponse createUser(UserRequest request);
    void deleteUser(String username);
    List<UserResponse> getAllUsers();
}
