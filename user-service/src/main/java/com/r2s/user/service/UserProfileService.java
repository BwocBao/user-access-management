package com.r2s.user.service;

import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.dto.UserResponse;

public interface UserProfileService {
    UserResponse updateUser(UpdateUserRequest request);
    UserResponse getUser(String username);
}
