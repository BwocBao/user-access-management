package com.r2s.user.unit.service;

import com.r2s.core.exception.ResourceNotFoundException;
import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.dto.UserResponse;
import com.r2s.user.entity.UserProfile;
import com.r2s.user.mapper.UserMapper;
import com.r2s.user.messaging.OutboxService;
import com.r2s.user.repository.UserProfileRepository;
import com.r2s.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserProfileRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private UserService userService;

    private UserProfile user1;
    private UserProfile user2;

    private UserResponse response1;
    private UserResponse response2;

    @BeforeEach
    void setUp() {
        user1 = UserProfile.builder()
                .id(1L)
                .username("username")
                .email("email")
                .fullName("fullname")
                .build();

        user2 = UserProfile.builder()
                .id(2L)
                .username("username2")
                .email("email2")
                .fullName("fullname2")
                .build();

        response1 = UserResponse.builder()
                .username("username")
                .fullName("fullname")
                .email("email")
                .build();

        response2 = UserResponse.builder()
                .username("username2")
                .fullName("fullname2")
                .email("email2")
                .build();
    }

    @Test
    void getAllUsers_shouldReturnListOfUserResponses() {
        when(userRepository.findAll()).thenReturn(List.of(user1, user2));
        when(userMapper.toUserResponse(user1)).thenReturn(response1);
        when(userMapper.toUserResponse(user2)).thenReturn(response2);

        List<UserResponse> result = userService.getAllUsers();

        assertEquals(2, result.size());
        assertEquals("username", result.get(0).getUsername());
        assertEquals("username2", result.get(1).getUsername());

        verify(userRepository).findAll();
        verify(userMapper).toUserResponse(user1);
        verify(userMapper).toUserResponse(user2);
        verifyNoMoreInteractions(userRepository, userMapper);
        verifyNoInteractions(outboxService);
    }

    @Test
    void getUserByUsername_shouldReturnUserResponse_whenUserExists() {
        when(userRepository.findByUsername(user1.getUsername()))
                .thenReturn(Optional.of(user1));
        when(userMapper.toUserResponse(user1))
                .thenReturn(response1);

        UserResponse result = userService.getUserByUsername(user1.getUsername());

        assertEquals(response1, result);

        verify(userRepository).findByUsername(user1.getUsername());
        verify(userMapper).toUserResponse(user1);
        verifyNoMoreInteractions(userRepository, userMapper);
        verifyNoInteractions(outboxService);
    }

    @Test
    void getUserByUsername_shouldThrowException_whenUserNotFound() {
        when(userRepository.findByUsername("username"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByUsername("username"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found: username");

        verify(userRepository).findByUsername("username");
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper, outboxService);
    }

    @Test
    void updateUser_shouldReturnUserResponse_whenUserExists() {
        when(userRepository.findByUsername(user1.getUsername()))
                .thenReturn(Optional.of(user1));

        ArgumentCaptor<UserProfile> userCaptor =
                ArgumentCaptor.forClass(UserProfile.class);

        when(userRepository.save(userCaptor.capture()))
                .thenAnswer(inv -> inv.getArgument(0));

        when(userMapper.toUserResponse(any(UserProfile.class)))
                .thenReturn(response1);

        UpdateUserRequest req = UpdateUserRequest.builder()
                .email("newemail")
                .fullName("newname")
                .build();

        UserResponse result = userService.updateUser(req, user1.getUsername());

        assertEquals(response1, result);

        UserProfile savedUser = userCaptor.getValue();
        assertEquals(user1.getUsername(), savedUser.getUsername());
        assertEquals("newemail", savedUser.getEmail());
        assertEquals("newname", savedUser.getFullName());

        verify(userRepository).findByUsername(user1.getUsername());
        verify(userRepository).save(any(UserProfile.class));
        verify(userMapper).toUserResponse(savedUser);
        verifyNoMoreInteractions(userRepository, userMapper);
        verifyNoInteractions(outboxService);
    }

    @Test
    void updateUser_shouldThrowException_whenUserNotFound() {
        when(userRepository.findByUsername("newuser"))
                .thenReturn(Optional.empty());

        UpdateUserRequest req = UpdateUserRequest.builder()
                .email("newemail")
                .fullName("newname")
                .build();

        assertThatThrownBy(() -> userService.updateUser(req, "newuser"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found: newuser");

        verify(userRepository).findByUsername("newuser");
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper, outboxService);
    }

    @Test
    void deleteUserByUsername_shouldDeleteUserAndSaveOutboxEvent_whenUserExists() {
        when(userRepository.existsByUsername(user1.getUsername()))
                .thenReturn(true);

        userService.deleteUserByUsername(user1.getUsername());

        verify(userRepository).existsByUsername(user1.getUsername());
        verify(userRepository).deleteByUsername(user1.getUsername());
        verify(outboxService).saveUserDeletedEvent(user1.getUsername());

        verifyNoMoreInteractions(userRepository, outboxService);
        verifyNoInteractions(userMapper);
    }

    @Test
    void deleteUserByUsername_shouldThrowException_whenUserNotFound() {
        when(userRepository.existsByUsername(user1.getUsername()))
                .thenReturn(false);

        assertThatThrownBy(() ->
                userService.deleteUserByUsername(user1.getUsername()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found: " + user1.getUsername());

        verify(userRepository).existsByUsername(user1.getUsername());
        verify(userRepository, never()).deleteByUsername(anyString());
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper, outboxService);
    }

    @Test
    void deleteUserByUsername_shouldPropagateException_whenDatabaseError() {
        when(userRepository.existsByUsername(user1.getUsername()))
                .thenReturn(true);

        doThrow(new RuntimeException("DB error"))
                .when(userRepository).deleteByUsername(user1.getUsername());

        assertThatThrownBy(() ->
                userService.deleteUserByUsername(user1.getUsername()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB error");

        verify(userRepository).existsByUsername(user1.getUsername());
        verify(userRepository).deleteByUsername(user1.getUsername());
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper, outboxService);
    }
}