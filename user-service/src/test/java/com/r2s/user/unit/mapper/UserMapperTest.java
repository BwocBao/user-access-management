package com.r2s.user.unit.mapper;

import com.r2s.core.entity.Role;
import com.r2s.core.entity.User;
import com.r2s.user.dto.UserResponse;
import com.r2s.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserMapperTest {

    private final UserMapper userMapper = new UserMapper();

    @Test
    void toUserResponse_shouldMapAllFieldsCorrectly() {

        // Arrange
        User user = User.builder().id(1L)
                .email("beo9@gmail.com")
                .username("beo9")
                .fullName("Beo Nguyen")
                .password("1234")
                .role(Role.ROLE_USER)
                .build();
        // Act
        UserResponse response = userMapper.toUserResponse(user);

        // Assert
        assertNotNull(response);
        assertEquals("beo9", response.getUsername());
        assertEquals("Beo Nguyen", response.getFullName());
        assertEquals("beo9@gmail.com", response.getEmail());
        assertEquals("ROLE_USER", response.getRole());
    }
}
