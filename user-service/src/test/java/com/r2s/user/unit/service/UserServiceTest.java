package com.r2s.user.unit.service;

import com.r2s.core.exception.ResourceNotFoundException;
import com.r2s.user.client.AuthServiceClient;
import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.dto.UserResponse;
import com.r2s.user.entity.UserProfile;
import com.r2s.user.mapper.UserMapper;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserProfileRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AuthServiceClient authServiceClient;

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
        assertEquals("username2", result.get(1).getUsername());

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
        UserResponse result = userService.getUserByUsername(user1.getUsername());

        // Assert
        assertEquals(response1, result);

        verify(userRepository).findByUsername(user1.getUsername());
        verify(userMapper).toUserResponse(user1);
        verifyNoMoreInteractions(userRepository, userMapper);
    }

    @Test
    void getUserByUsername_shouldCreateUser_whenNotFound() {

        when(userRepository.findByUsername("username"))
                .thenReturn(Optional.empty());

        ArgumentCaptor<UserProfile> userCaptor =
                ArgumentCaptor.forClass(UserProfile.class);

        when(userRepository.saveAndFlush(userCaptor.capture()))
                .thenAnswer(inv -> inv.getArgument(0));

        when(userMapper.toUserResponse(any(UserProfile.class)))
                .thenReturn(response1);

        // Act
        UserResponse result = userService.getUserByUsername("username");

        // Assert result
        assertEquals(response1, result);

        UserProfile savedUser = userCaptor.getValue();
        assertEquals("username", savedUser.getUsername());
        assertEquals("", savedUser.getFullName());
        assertEquals("", savedUser.getEmail());

        verify(userRepository).findByUsername("username");
        verify(userRepository).saveAndFlush(any(UserProfile.class));
        verify(userMapper).toUserResponse(savedUser);

        verifyNoMoreInteractions(userRepository, userMapper);
    }

    @Test
    void updateUser_shouldReturnUserResponse_whenUserExists() {
        when(userRepository.findByUsername(user1.getUsername())).thenReturn(Optional.of(user1));
        ArgumentCaptor<UserProfile> userCaptor = ArgumentCaptor.forClass(UserProfile.class);

        when(userRepository.save(userCaptor.capture()))
                .thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toUserResponse(any(UserProfile.class))).thenReturn(response1);

        UpdateUserRequest updateUserRequest = UpdateUserRequest.builder()
                .email("newemail")
                .fullName("newname").build();
//        Act
        UserResponse userResponse=
                userService.updateUser(updateUserRequest,user1.getUsername());

        // Assert result
        assertEquals(response1, userResponse);

        // Assert user created
        UserProfile savedUser = userCaptor.getValue();
        assertEquals(user1.getUsername(), savedUser.getUsername());
        assertEquals("newemail", savedUser.getEmail());
        assertEquals("newname", savedUser.getFullName());

        verify(userRepository).findByUsername(user1.getUsername());
        verify(userRepository).save(any(UserProfile.class));
        verify(userMapper).toUserResponse(savedUser);
        verifyNoMoreInteractions(userRepository, userMapper);
    }


    @Test
    void updateUser_shouldCreateUserAndSaveAndReturnUserResponse_whenNotFound() {

        when(userRepository.findByUsername("newuser"))
                .thenReturn(Optional.empty());

        ArgumentCaptor<UserProfile> userCaptor =
                ArgumentCaptor.forClass(UserProfile.class);

        when(userRepository.saveAndFlush(any(UserProfile.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(userRepository.save(userCaptor.capture()))
                .thenAnswer(inv -> inv.getArgument(0));

        when(userMapper.toUserResponse(any(UserProfile.class)))
                .thenReturn(response1);

        UpdateUserRequest req = UpdateUserRequest.builder()
                .email("newemail")
                .fullName("newname")
                .build();

        // Act
        UserResponse result = userService.updateUser(req, "newuser");

        // Assert
        assertEquals(response1, result);

        UserProfile savedUser = userCaptor.getValue();
        assertEquals("newuser", savedUser.getUsername());
        assertEquals("newemail", savedUser.getEmail());
        assertEquals("newname", savedUser.getFullName());

        verify(userRepository).findByUsername("newuser");
        verify(userRepository).saveAndFlush(any(UserProfile.class));
        verify(userRepository).save(any(UserProfile.class));
        verify(userMapper).toUserResponse(savedUser);

        verifyNoMoreInteractions(userRepository, userMapper);
    }

    @Test
    void deleteUserByUsername_shouldDeleteUser_whenUserExists() {

        when(userRepository.existsByUsername(user1.getUsername()))
                .thenReturn(true);

        // Act
        userService.deleteUserByUsername(user1.getUsername());

        // Assert
        verify(userRepository).existsByUsername(user1.getUsername());
        verify(userRepository).deleteByUsername(user1.getUsername());
        verify(authServiceClient).deleteUser(user1.getUsername());

        verifyNoMoreInteractions(userRepository, authServiceClient);
        verifyNoInteractions(userMapper);
    }

    @Test
    void deleteUserByUsername_shouldThrowException_whenUserNotFound() {
        // Arrange
        when(userRepository.existsByUsername(user1.getUsername()))
                .thenReturn(false);

        // Act + Assert
        assertThatThrownBy(() ->
                userService.deleteUserByUsername(user1.getUsername()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found: " + user1.getUsername());

        verify(userRepository).existsByUsername(user1.getUsername());
        verify(userRepository, never()).deleteByUsername(anyString());

        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper);
    }

    @Test
    void deleteUserByUsername_shouldPropagateException_whenDatabaseError() {
        // Arrange
        when(userRepository.existsByUsername(user1.getUsername()))
                .thenReturn(true);

        doThrow(new RuntimeException("DB error"))
                .when(userRepository).deleteByUsername(user1.getUsername());

        // Act + Assert
        assertThatThrownBy(() ->
                userService.deleteUserByUsername(user1.getUsername()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB error");

        verify(userRepository).existsByUsername(user1.getUsername());
        verify(userRepository).deleteByUsername(user1.getUsername());

        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(userMapper);
    }
}
//Khi nào NÊN test PropagateException_whenDatabaseError?
//✅ Test khi: Service CÓ logic xử lý exception
//Ví dụ 1: map exception
//try {
//        userRepository.save(user);
//} catch (DataIntegrityViolationException e) {
//        throw new CustomException("Username exist");
//}
//➡️ BẮT BUỘC TEST
//
//shouldThrowCustomException_whenDuplicateUsername()
//
//Ví dụ 2: rollback / transaction
//@Transactional
//public void createUser() {
//    repo.save(user);
//    mailService.send();
//}
//➡️ Test khi: save fail → mail không được gọi hoặc ngược lại
//
//Ví dụ 3: retry / fallback / log
//try {
//        repo.save(user);
//} catch (Exception e) {
//        log.error(...);
//        throw e;
//}
//➡️ Test để đảm bảo: log đúng / exception không bị nuốt
