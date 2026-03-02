package com.r2s.user.unit.service;

import com.r2s.core.entity.Role;
import com.r2s.core.entity.User;
import com.r2s.core.repository.UserRepository;
import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.dto.UserResponse;
import com.r2s.user.mapper.UserMapper;
import com.r2s.user.service.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    // =========================
    // CONSTANTS
    // =========================
    private static final String USERNAME = "username";
    private static final String USERNAME_2 = "username2";
    private static final String EMAIL = "email";
    private static final String FULLNAME = "fullname";

    // =========================
    // MOCKS
    // =========================
    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User user;
    private UserResponse response;

    @BeforeEach
    void setUp() {
        user = buildUser(USERNAME, EMAIL, FULLNAME, Role.ROLE_USER);
        response = buildResponse(USERNAME, EMAIL, FULLNAME, "ROLE_USER");
        SecurityContextHolder.clearContext();
    }

    // =========================
    // GET ALL USERS
    // =========================
    @Nested
    class GetAllUsersTests {

        @Test
        void shouldReturnListOfUsers() {
            when(userRepository.findAll()).thenReturn(List.of(user));
            when(userMapper.toUserResponse(user)).thenReturn(response);

            List<UserResponse> result = userService.getAllUsers();

            assertEquals(1, result.size());
            assertEquals(USERNAME, result.get(0).getUsername());

            verify(userRepository).findAll();
            verify(userMapper).toUserResponse(user);
        }
    }

    // =========================
    // GET USER
    // =========================
    @Nested
    class GetUserTests {

        @Test
        void shouldReturnUser_whenUserExists() {
            when(userRepository.findByUsername(USERNAME))
                    .thenReturn(Optional.of(user));
            when(userMapper.toUserResponse(user))
                    .thenReturn(response);

            UserResponse result = userService.getUser(USERNAME);

            assertEquals(response, result);

            verify(userRepository).findByUsername(USERNAME);
            verify(userMapper).toUserResponse(user);
        }

        @Test
        void shouldThrowException_whenUserNotFound() {
            when(userRepository.findByUsername(USERNAME))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUser(USERNAME))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");

            verify(userRepository).findByUsername(USERNAME);
            verifyNoInteractions(userMapper);
        }
    }

    // =========================
    // UPDATE USER
    // =========================
    @Nested
    class UpdateUserTests {

        @Test
        void shouldUpdateUser_whenUserExists() {
            mockSecurity(USERNAME, "ROLE_USER");

            when(userRepository.findByUsername(USERNAME))
                    .thenReturn(Optional.of(user));

            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userMapper.toUserResponse(any()))
                    .thenReturn(response);

            UpdateUserRequest request = UpdateUserRequest.builder()
                    .email("newemail")
                    .fullName("newname")
                    .build();

            UserResponse result = userService.updateUser(request);

            assertEquals(response, result);

            verify(userRepository).findByUsername(USERNAME);
            verify(userRepository).save(any(User.class));
            verify(userMapper).toUserResponse(any(User.class));
        }

        @Test
        void shouldCreateUser_whenUserNotFound() {
            mockSecurity("newuser", "ROLE_USER");

            when(userRepository.findByUsername("newuser"))
                    .thenReturn(Optional.empty());

            when(userRepository.save(any()))
                    .thenAnswer(inv -> inv.getArgument(0));

            when(userMapper.toUserResponse(any()))
                    .thenReturn(response);

            UpdateUserRequest request = UpdateUserRequest.builder()
                    .email("newemail")
                    .fullName("newname")
                    .build();

            userService.updateUser(request);

            verify(userRepository).findByUsername("newuser");
            verify(userRepository, times(2)).save(any(User.class));
        }
    }

    // =========================
    // DELETE USER
    // =========================
    @Nested
    class DeleteUserTests {

        @Test
        void shouldDeleteUser_whenExists() {
            when(userRepository.existsByUsername(USERNAME))
                    .thenReturn(true);

            userService.deleteUser(USERNAME);

            verify(userRepository).existsByUsername(USERNAME);
            verify(userRepository).deleteByUsername(USERNAME);
        }

        @Test
        void shouldThrowException_whenNotFound() {
            when(userRepository.existsByUsername(USERNAME))
                    .thenReturn(false);

            assertThatThrownBy(() ->
                    userService.deleteUser(USERNAME))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");

            verify(userRepository).existsByUsername(USERNAME);
        }
    }

    // =========================
    // HELPER
    // =========================
    private void mockSecurity(String username, String role) {
        Authentication authentication = mock(Authentication.class);
        SecurityContext context = mock(SecurityContext.class);

        when(authentication.getName()).thenReturn(username);
        when(authentication.getAuthorities())
                .thenReturn((Collection) Collections.singletonList(
                        new SimpleGrantedAuthority(role)
                ));

        when(context.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(context);
    }

    private User buildUser(String username, String email, String fullname, Role role) {
        return User.builder()
                .username(username)
                .email(email)
                .fullName(fullname)
                .role(role)
                .build();
    }

    private UserResponse buildResponse(String username, String email,
                                       String fullname, String role) {
        return UserResponse.builder()
                .username(username)
                .email(email)
                .fullName(fullname)
                .role(role)
                .build();
    }
}

//@ExtendWith(MockitoExtension.class)
//class UserServiceUnitTest {
//
//    @Mock
//    private UserRepository userRepository;
//
//    @Mock
//    private UserMapper userMapper;
//
//    @InjectMocks
//    private UserServiceImpl userService;
//
//    private User user1;
//    private User user2;
//
//    private UserResponse response1;
//    private UserResponse response2;
//
//    @BeforeEach
//    void setUp() {
//        user1 = User.builder()
//                .id(1L)
//                .username("username")
//                .email("email")
//                .fullName("fullname")
//                .role(Role.ROLE_USER)
//                .build();
//
//        user2 = User.builder()
//                .id(2L)
//                .username("username2")
//                .email("email2")
//                .fullName("fullname2")
//                .role(Role.ROLE_ADMIN)
//                .build();
//
//        response1 = UserResponse.builder()
//                .username("username")
//                .fullName("fullname")
//                .email("email")
//                .role("ROLE_USER")
//                .build();
//
//        response2 = UserResponse.builder()
//                .username("username2")
//                .fullName("fullname2")
//                .email("email2")
//                .role("ROLE_ADMIN")
//                .build();
//
//        SecurityContextHolder.clearContext();
//    }
//
//    // =========================
//    // Helper mock security
//    // =========================
//    private void mockSecurityContext(String username, String role) {
//        Authentication authentication = mock(Authentication.class);
//        SecurityContext securityContext = mock(SecurityContext.class);
//
//        when(authentication.getName()).thenReturn(username);
//        when(authentication.getAuthorities())
//                .thenReturn((Collection) Collections.singletonList(
//                        new SimpleGrantedAuthority(role)
//                ));
//        when(securityContext.getAuthentication()).thenReturn(authentication);
//
//        SecurityContextHolder.setContext(securityContext);
//    }
//
//    // =========================
//    // getAllUsers
//    // =========================
//    @Test
//    void getAllUsers_shouldReturnListOfUserResponses() {
//        when(userRepository.findAll())
//                .thenReturn(List.of(user1, user2));
//
//        when(userMapper.toUserResponse(user1)).thenReturn(response1);
//        when(userMapper.toUserResponse(user2)).thenReturn(response2);
//
//        List<UserResponse> result = userService.getAllUsers();
//
//        assertEquals(2, result.size());
//        assertEquals("username", result.get(0).getUsername());
//        assertEquals("username2", result.get(1).getUsername());
//
//        verify(userRepository).findAll();
//        verify(userMapper).toUserResponse(user1);
//        verify(userMapper).toUserResponse(user2);
//        verifyNoMoreInteractions(userRepository, userMapper);
//    }
//
//    // =========================
//    // getUser
//    // =========================
//    @Test
//    void getUser_shouldReturnUserResponse_whenUserExists() {
//        when(userRepository.findByUsername("username"))
//                .thenReturn(Optional.of(user1));
//        when(userMapper.toUserResponse(user1))
//                .thenReturn(response1);
//
//        UserResponse result = userService.getUser("username");
//
//        assertEquals(response1, result);
//
//        verify(userRepository).findByUsername("username");
//        verify(userMapper).toUserResponse(user1);
//        verifyNoMoreInteractions(userRepository, userMapper);
//    }
//
//    @Test
//    void getUser_shouldThrowException_whenUserNotFound() {
//        when(userRepository.findByUsername("username"))
//                .thenReturn(Optional.empty());
//
//        assertThatThrownBy(() ->
//                userService.getUser("username"))
//                .isInstanceOf(RuntimeException.class)
//                .hasMessageContaining("User not found");
//
//        verify(userRepository).findByUsername("username");
//        verifyNoMoreInteractions(userRepository);
//        verifyNoInteractions(userMapper);
//    }
//
//    // =========================
//    // updateUser
//    // =========================
//    @Test
//    void updateUser_shouldUpdateExistingUser() {
//        mockSecurityContext("username", "ROLE_USER");
//
//        when(userRepository.findByUsername("username"))
//                .thenReturn(Optional.of(user1));
//
//        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
//
//        when(userRepository.save(captor.capture()))
//                .thenAnswer(inv -> inv.getArgument(0));
//
//        when(userMapper.toUserResponse(any(User.class)))
//                .thenReturn(response1);
//
//        UpdateUserRequest request = UpdateUserRequest.builder()
//                .email("newemail")
//                .fullName("newname")
//                .build();
//
//        UserResponse result = userService.updateUser(request);
//
//        assertEquals(response1, result);
//
//        User saved = captor.getValue();
//        assertEquals("username", saved.getUsername());
//        assertEquals("newemail", saved.getEmail());
//        assertEquals("newname", saved.getFullName());
//        assertEquals(Role.ROLE_USER, saved.getRole());
//
//        verify(userRepository).findByUsername("username");
//        verify(userRepository).save(any(User.class));
//        verify(userMapper).toUserResponse(saved);
//        verifyNoMoreInteractions(userRepository, userMapper);
//    }
//
//    @Test
//    void updateUser_shouldCreateUser_whenNotFound() {
//        mockSecurityContext("newuser", "ROLE_USER");
//
//        when(userRepository.findByUsername("newuser"))
//                .thenReturn(Optional.empty());
//
//        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
//
//        when(userRepository.save(captor.capture()))
//                .thenAnswer(inv -> inv.getArgument(0));
//
//        when(userMapper.toUserResponse(any(User.class)))
//                .thenReturn(response1);
//
//        UpdateUserRequest request = UpdateUserRequest.builder()
//                .email("newemail")
//                .fullName("newname")
//                .build();
//
//        UserResponse result = userService.updateUser(request);
//
//        assertEquals(response1, result);
//
//        User saved = captor.getValue();
//        assertEquals("newuser", saved.getUsername());
//        assertEquals("newemail", saved.getEmail());
//        assertEquals("newname", saved.getFullName());
//        assertEquals("", saved.getPassword());
//        assertEquals(Role.ROLE_USER, saved.getRole());
//
//        verify(userRepository).findByUsername("newuser");
//        verify(userRepository, times(2)).save(any(User.class));
//        verify(userMapper).toUserResponse(saved);
//        verifyNoMoreInteractions(userRepository, userMapper);
//    }
//
//    // =========================
//    // deleteUser
//    // =========================
//    @Test
//    void deleteUser_shouldDeleteUser_whenExists() {
//        when(userRepository.existsByUsername("username"))
//                .thenReturn(true);
//
//        userService.deleteUser("username");
//
//        verify(userRepository).existsByUsername("username");
//        verify(userRepository).deleteByUsername("username");
//        verifyNoMoreInteractions(userRepository);
//        verifyNoInteractions(userMapper);
//    }
//
//    @Test
//    void deleteUser_shouldThrowException_whenNotFound() {
//        when(userRepository.existsByUsername("username"))
//                .thenReturn(false);
//
//        assertThatThrownBy(() ->
//                userService.deleteUser("username"))
//                .isInstanceOf(RuntimeException.class)
//                .hasMessageContaining("User not found");
//
//        verify(userRepository).existsByUsername("username");
//        verifyNoMoreInteractions(userRepository);
//        verifyNoInteractions(userMapper);
//    }
//
//    @Test
//    void deleteUser_shouldThrowWrappedException_whenDatabaseFails() {
//        when(userRepository.existsByUsername("username"))
//                .thenReturn(true);
//
//        doThrow(new RuntimeException("DB error"))
//                .when(userRepository).deleteByUsername("username");
//
//        assertThatThrownBy(() ->
//                userService.deleteUser("username"))
//                .isInstanceOf(RuntimeException.class)
//                .hasMessageContaining("User deletion failed");
//
//        verify(userRepository).existsByUsername("username");
//        verify(userRepository).deleteByUsername("username");
//        verifyNoMoreInteractions(userRepository);
//        verifyNoInteractions(userMapper);
//    }
//}



//package com.r2s.user.unit.service;
//
//import com.r2s.core.entity.Role;
//import com.r2s.core.entity.User;
//import com.r2s.core.exception.ResourceNotFoundExecption;
//import com.r2s.core.repository.UserRepository;
//import com.r2s.user.dto.UpdateUserRequest;
//import com.r2s.user.dto.UserResponse;
//import com.r2s.user.mapper.UserMapper;
//import com.r2s.user.service.UserService;
//import com.r2s.user.service.UserServiceImpl;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.ArgumentCaptor;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//
//import java.util.List;
//import java.util.Optional;
//
//import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
//import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class UserServiceUnitTest {
//
//    @Mock
//    private UserRepository userRepository;
//
//    @Mock
//    private UserMapper userMapper;
//
//
//    @InjectMocks
//    private UserServiceImpl userService;
//
//    private User user1;
//    private User user2;
//
//    private UserResponse response1;
//    private UserResponse response2;
//
//
//    @BeforeEach
//    void setUp() {
//        user1 = User.builder()
//                .id(1L)
//                .username("username")
//                .email("email")
//                .fullName("fullname")
//                .role(Role.ROLE_USER)
//                .build();
//
//        user2 = User.builder()
//                .id(2L)
//                .username("username2")
//                .email("email2")
//                .fullName("fullname2")
//                .role(Role.ROLE_ADMIN)
//                .build();
//
//        response1 = UserResponse.builder()
//                .username("username")
//                .fullName("fullname")
//                .email("email")
//                .role("ROLE_USER")
//                .build();
//
//        response2 = UserResponse.builder()
//                .username("username2")
//                .fullName("fullname2")
//                .email("email2")
//                .role("ROLE_ADMIN")
//                .build();
//    }
//
//
//    @Test
//    void getAllUsers_shouldReturnListOfUserResponses() {
//        // Arrange
//        when(userRepository.findAll())
//                .thenReturn(List.of(user1, user2));
//
//        when(userMapper.toUserResponse(user1))
//                .thenReturn(response1);
//        when(userMapper.toUserResponse(user2))
//                .thenReturn(response2);
//
//        // Act
//        List<UserResponse> result = userService.getAllUsers();
//
//        // Assert (output)
//        assertEquals(2, result.size());
//        assertEquals("username", result.get(0).getUsername());
//        assertEquals("ROLE_USER", result.get(0).getRole());
//        assertEquals("username2", result.get(1).getUsername());
//        assertEquals("ROLE_ADMIN", result.get(1).getRole());
//
//        // Assert (interaction)
//        verify(userRepository).findAll();
//        verify(userMapper).toUserResponse(user1);
//        verify(userMapper).toUserResponse(user2);
//
//        verifyNoMoreInteractions(userRepository, userMapper);
//    }
//
//    @Test
//    void getUserByUsername_shouldReturnUserResponse_whenUserExists() {
//        // Arrange
//        when(userRepository.findByUsername(user1.getUsername()))
//                .thenReturn(Optional.of(user1));
//
//        when(userMapper.toUserResponse(user1))
//                .thenReturn(response1);
//
//        // Act
//        UserResponse result = userService.getUser(user1.getUsername());
//
//        // Assert
//        assertEquals(response1, result);
//
//        verify(userRepository).findByUsername(user1.getUsername());
//        verify(userMapper).toUserResponse(user1);
//        verifyNoMoreInteractions(userRepository, userMapper);
//    }
//
////    @Test
////    void getUserByUsername_shouldCreateUser_whenNotFound() {
////        when(userRepository.findByUsername("username"))
////                .thenReturn(Optional.empty());
////
////        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
////
////        when(userRepository.save(userCaptor.capture()))
////                .thenAnswer(inv -> inv.getArgument(0));
////
//////      💡 Ở unit test service, ta KHÔNG test mapper → Mapper được coi là đã đúng
//////      ➡️ Nên mapper:nhận user nào cũng trả về response1 là chấp nhận được
////        when(userMapper.toUserResponse(any(User.class)))
////                .thenReturn(response1);
////
////        // Act
////        UserResponse result =
////                userService.getUser("username");
////
////        // Assert result
////        assertEquals(response1, result);
////
////        // Assert user created
////        User savedUser = userCaptor.getValue();
////        assertEquals("username", savedUser.getUsername());
////        assertEquals(Role.ROLE_USER, savedUser.getRole());
////
////        verify(userRepository).findByUsername("username");
////        verify(userRepository).save(any(User.class));
////        verify(userMapper).toUserResponse(savedUser);
////        verifyNoMoreInteractions(userRepository, userMapper);
////    }
//
//    @Test
//    void updateUser_shouldReturnUserResponse_whenUserExists() {
//        when(userRepository.findByUsername(user1.getUsername())).thenReturn(Optional.of(user1));
//        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
//
//        when(userRepository.save(userCaptor.capture()))
//                .thenAnswer(inv -> inv.getArgument(0));
//        when(userMapper.toUserResponse(any(User.class))).thenReturn(response1);
//
//        UpdateUserRequest updateUserRequest = UpdateUserRequest.builder()
//                .email("newemail")
//                .fullName("newname").build();
////        Act
//        UserResponse userResponse=
//                userService.updateUser(updateUserRequest);
//
//        // Assert result
//        assertEquals(response1, userResponse);
//
//        // Assert user created
//        User savedUser = userCaptor.getValue();
//        assertEquals(user1.getUsername(), savedUser.getUsername());
//        assertEquals(Role.ROLE_USER, savedUser.getRole());
//        assertEquals("newemail", savedUser.getEmail());
//        assertEquals("newname", savedUser.getFullName());
//
//        verify(userRepository).findByUsername(user1.getUsername());
//        verify(userRepository).save(any(User.class));
//        verify(userMapper).toUserResponse(savedUser);
//        verifyNoMoreInteractions(userRepository, userMapper);
//    }
//
//
//    @Test
//    void updateUser_shouldCreateUserAndSaveAndReturnUserResponse_whenNotFound() {
//        // Arrange
//        when(userRepository.findByUsername("newuser"))
//                .thenReturn(Optional.empty());
//
//        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
//
//        when(userRepository.save(userCaptor.capture()))
//                .thenAnswer(inv -> inv.getArgument(0));
//
//        when(userMapper.toUserResponse(any(User.class)))
//                .thenReturn(response1);
//
//        UpdateUserRequest req = UpdateUserRequest.builder()
//                .email("newemail")
//                .fullName("newname")
//                .build();
//
//        // Act
//        UserResponse result =
//                userService.updateUser(req);
//
//        // Assert result
//        assertEquals(response1, result);
//
//        // Assert created user
//        User savedUser = userCaptor.getValue();
//        assertEquals("newuser", savedUser.getUsername());
//        assertEquals(Role.ROLE_USER, savedUser.getRole());
//        assertEquals("newemail", savedUser.getEmail());
//        assertEquals("newname", savedUser.getFullName());
//        assertEquals("", savedUser.getPassword());
//
//        // Verify interactions
//        verify(userRepository).findByUsername("newuser");
//        verify(userRepository).save(any(User.class));
//        verify(userMapper).toUserResponse(savedUser);
//        verifyNoMoreInteractions(userRepository, userMapper);
//    }
//
//    @Test
//    void deleteUserByUsername_shouldDeleteUser_whenUserExists() {
//        // Arrange
//        when(userRepository.existsByUsername(user1.getUsername()))
//                .thenReturn(true);
//
//        // Act
//        userService.deleteUser(user1.getUsername());
//
//        // Assert
//        verify(userRepository).existsByUsername(user1.getUsername());
//        verify(userRepository).deleteByUsername(user1.getUsername());
//
//        verifyNoMoreInteractions(userRepository);
//        verifyNoInteractions(userMapper);
//    }
//
//    @Test
//    void deleteUserByUsername_shouldThrowException_whenUserNotFound() {
//        // Arrange
//        when(userRepository.existsByUsername(user1.getUsername()))
//                .thenReturn(false);
//
//        // Act + Assert
//        assertThatThrownBy(() ->
//                userService.deleteUser(user1.getUsername()))
//                .isInstanceOf(ResourceNotFoundExecption.class)
//                .hasMessageContaining("User not found: " + user1.getUsername());
//
//        verify(userRepository).existsByUsername(user1.getUsername());
//        verify(userRepository, never()).deleteByUsername(anyString());
//
//        verifyNoMoreInteractions(userRepository);
//        verifyNoInteractions(userMapper);
//    }
//
//    @Test
//    void deleteUserByUsername_shouldPropagateException_whenDatabaseError() {
//        // Arrange
//        when(userRepository.existsByUsername(user1.getUsername()))
//                .thenReturn(true);
//
//        doThrow(new RuntimeException("DB error"))
//                .when(userRepository).deleteByUsername(user1.getUsername());
//
//        // Act + Assert
//        assertThatThrownBy(() ->
//                userService.deleteUser(user1.getUsername()))
//                .isInstanceOf(RuntimeException.class)
//                .hasMessageContaining("DB error");
//
//        verify(userRepository).existsByUsername(user1.getUsername());
//        verify(userRepository).deleteByUsername(user1.getUsername());
//
//        verifyNoMoreInteractions(userRepository);
//        verifyNoInteractions(userMapper);
//    }
//    private void mockSecurityContext(String username, String role) {
//        var authentication = mock(org.springframework.security.core.Authentication.class);
//        var securityContext = mock(org.springframework.security.core.context.SecurityContext.class);
//
//        when(authentication.getName()).thenReturn(username);
//        when(authentication.getAuthorities())
//                .thenReturn(List.of(() -> role));
//
//        when(securityContext.getAuthentication()).thenReturn(authentication);
//
//        org.springframework.security.core.context.SecurityContextHolder
//                .setContext(securityContext);
//    }
//}
//
//
////Khi nào NÊN test PropagateException_whenDatabaseError?
////✅ Test khi: Service CÓ logic xử lý exception
////Ví dụ 1: map exception
////try {
////        userRepository.save(user);
////} catch (DataIntegrityViolationException e) {
////        throw new CustomException("Username exist");
////}
////➡️ BẮT BUỘC TEST
////
////shouldThrowCustomException_whenDuplicateUsername()
////
////Ví dụ 2: rollback / transaction
////@Transactional
////public void createUser() {
////    repo.save(user);
////    mailService.send();
////}
////➡️ Test khi: save fail → mail không được gọi hoặc ngược lại
////
////Ví dụ 3: retry / fallback / log
////try {
////        repo.save(user);
////} catch (Exception e) {
////        log.error(...);
////        throw e;
////}
////➡️ Test để đảm bảo: log đúng / exception không bị nuốt
