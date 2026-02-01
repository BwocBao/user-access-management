package com.r2s.user.service;

import com.r2s.core.entity.Role;
import com.r2s.core.entity.User;
import com.r2s.core.repository.UserRepository;
import com.r2s.user.dto.UserResponse;
import com.r2s.user.mapper.UserMapper;
import org.hibernate.mapping.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;


    @InjectMocks
    private UserService userService;

    private User user1;
    private User user2;

    private UserResponse response1;
    private UserResponse response2;

    
    @BeforeEach
    void setUp() {
        user1 = User.builder()
                .id(1L)
                .username("username")
                .email("email")
                .fullName("fullname")
                .role(Role.ROLE_USER)
                .build();

        user2 = User.builder()
                .id(2L)
                .username("username2")
                .email("email2")
                .fullName("fullname2")
                .role(Role.ROLE_ADMIN)
                .build();

        response1 = UserResponse.builder()
                .username("username")
                .fullName("fullname")
                .email("email")
                .role("ROLE_USER")
                .build();

        response2 = UserResponse.builder()
                .username("username2")
                .fullName("fullname2")
                .email("email2")
                .role("ROLE_ADMIN")
                .build();
    }


    @Test
    void getAllUsers_shouldReturnListOfUserResponses() {
        // Arrange
        when(userRepository.findAll())
                .thenReturn(List.of(user1, user2));

        when(userMapper.toUserResponse(user1))
                .thenReturn(response1);
        when(userMapper.toUserResponse(user2))
                .thenReturn(response2);

        // Act
        List<UserResponse> result = userService.getAllUsers();

        // Assert (output)
        assertEquals(2, result.size());
        assertEquals("username", result.get(0).getUsername());
        assertEquals("ROLE_USER", result.get(0).getRole());
        assertEquals("username2", result.get(1).getUsername());
        assertEquals("ROLE_ADMIN", result.get(1).getRole());

        // Assert (interaction)
        verify(userRepository).findAll();
        verify(userMapper).toUserResponse(user1);
        verify(userMapper).toUserResponse(user2);

        verifyNoMoreInteractions(userRepository, userMapper);
    }

    @Test
    void getUserByUsername_shouldReturnUserResponse_whenUserExists() {
        // Arrange
        when(userRepository.findByUsername(user1.getUsername()))
                .thenReturn(Optional.of(user1));

        when(userMapper.toUserResponse(user1))
                .thenReturn(response1);

        // Act
        UserResponse result = userService.getUserByUsername(user1.getUsername(),user1.getRole().toString());

        // Assert
        assertEquals(response1, result);

        verify(userRepository).findByUsername(user1.getUsername());
        verify(userMapper).toUserResponse(user1);
        verifyNoMoreInteractions(userRepository, userMapper);
    }

    @Test
    void getUserByUsername_shouldCreateUser_whenNotFound() {
        when(userRepository.findByUsername("newuser"))
                .thenReturn(Optional.empty());

        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(userMapper.toUserResponse(any(User.class)))
                .thenReturn(response1);

        UserResponse result = userService.getUserByUsername("newuser","ROLE_USER");

        assertEquals(response1, result);

        verify(userRepository).findByUsername("newuser");
        verify(userRepository).save(any(User.class));
        verify(userMapper).toUserResponse(any(User.class));
    }

}
