package com.r2s.user.service;

import com.r2s.core.entity.Role;
import com.r2s.core.entity.User;
import com.r2s.core.exception.ResourceNotFoundExecption;
import com.r2s.core.repository.UserRepository;
import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.dto.UserResponse;
import com.r2s.user.mapper.UserMapper;
import org.hibernate.mapping.Collection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
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
        when(userRepository.findByUsername("username"))
                .thenReturn(Optional.empty());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        when(userRepository.save(userCaptor.capture()))
                .thenAnswer(inv -> inv.getArgument(0));

//      💡 Ở unit test service, ta KHÔNG test mapper → Mapper được coi là đã đúng
//      ➡️ Nên mapper:nhận user nào cũng trả về response1 là chấp nhận được
        when(userMapper.toUserResponse(any(User.class)))
                .thenReturn(response1);

        // Act
        UserResponse result =
                userService.getUserByUsername("username", "ROLE_USER");

        // Assert result
        assertEquals(response1, result);

        // Assert user created
        User savedUser = userCaptor.getValue();
        assertEquals("username", savedUser.getUsername());
        assertEquals(Role.ROLE_USER, savedUser.getRole());

        verify(userRepository).findByUsername("username");
        verify(userRepository).save(any(User.class));
        verify(userMapper).toUserResponse(savedUser);
        verifyNoMoreInteractions(userRepository, userMapper);
    }

    @Test
    void updateUser_shouldReturnUserResponse_whenUserExists() {
        when(userRepository.findByUsername(user1.getUsername())).thenReturn(Optional.of(user1));
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        when(userRepository.save(userCaptor.capture()))
                .thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toUserResponse(any(User.class))).thenReturn(response1);

        UpdateUserRequest updateUserRequest = UpdateUserRequest.builder()
                .email("newemail")
                .fullName("newname").build();
//        Act
        UserResponse userResponse=
                userService.updateUser(updateUserRequest,user1.getUsername(),user1.getRole().toString());

        // Assert result
        assertEquals(response1, userResponse);

        // Assert user created
        User savedUser = userCaptor.getValue();
        assertEquals(user1.getUsername(), savedUser.getUsername());
        assertEquals(Role.ROLE_USER, savedUser.getRole());
        assertEquals("newemail", savedUser.getEmail());
        assertEquals("newname", savedUser.getFullName());

        verify(userRepository).findByUsername(user1.getUsername());
        verify(userRepository).save(any(User.class));
        verify(userMapper).toUserResponse(savedUser);
        verifyNoMoreInteractions(userRepository, userMapper);
    }


    @Test
    void updateUser_shouldCreateUserAndSaveAndReturnUserResponse_whenNotFound() {
        // Arrange
        when(userRepository.findByUsername("newuser"))
                .thenReturn(Optional.empty());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

        when(userRepository.save(userCaptor.capture()))
                .thenAnswer(inv -> inv.getArgument(0));

        when(userMapper.toUserResponse(any(User.class)))
                .thenReturn(response1);

        UpdateUserRequest req = UpdateUserRequest.builder()
                .email("newemail")
                .fullName("newname")
                .build();

        // Act
        UserResponse result =
                userService.updateUser(req, "newuser", "ROLE_USER");

        // Assert result
        assertEquals(response1, result);

        // Assert created user
        User savedUser = userCaptor.getValue();
        assertEquals("newuser", savedUser.getUsername());
        assertEquals(Role.ROLE_USER, savedUser.getRole());
        assertEquals("newemail", savedUser.getEmail());
        assertEquals("newname", savedUser.getFullName());
        assertEquals("", savedUser.getPassword());

        // Verify interactions
        verify(userRepository).findByUsername("newuser");
        verify(userRepository).save(any(User.class));
        verify(userMapper).toUserResponse(savedUser);
        verifyNoMoreInteractions(userRepository, userMapper);
    }

    @Test
    void deleteUserByUsername_shouldDeleteUser_whenUserExists() {
        // Arrange
        when(userRepository.existsByUsername(user1.getUsername()))
                .thenReturn(true);

        // Act
        userService.deleteUserByUsername(user1.getUsername());

        // Assert
        verify(userRepository).existsByUsername(user1.getUsername());
        verify(userRepository).deleteByUsername(user1.getUsername());

        verifyNoMoreInteractions(userRepository);
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
                .isInstanceOf(ResourceNotFoundExecption.class)
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
