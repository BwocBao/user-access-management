package com.r2s.user.unit.mapper;

import com.r2s.core.entity.Role;
import com.r2s.user.dto.UserResponse;
import com.r2s.user.entity.UserProfile;
import com.r2s.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserMapperTest {

    private final UserMapper userMapper = new UserMapper();

    @Test
    void toUserResponse_shouldMapAllFieldsCorrectly() {

        // Arrange
        UserProfile user = UserProfile.builder()
                .username("beo9")
                .fullName("Beo Nguyen")
                .email("beo9@gmail.com")
                .build();

        // Act
        UserResponse response = userMapper.toUserResponse(user);

        // Assert
        assertNotNull(response);
        assertEquals("beo9", response.getUsername());
        assertEquals("Beo Nguyen", response.getFullName());
        assertEquals("beo9@gmail.com", response.getEmail());
    }
}
