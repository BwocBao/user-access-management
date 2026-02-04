package com.r2s.auth.unit.service;

import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.dto.RegisterRequest;
import com.r2s.auth.dto.RegisterRoleRequest;
import com.r2s.auth.service.AuthService;
import com.r2s.core.entity.Role;
import com.r2s.core.entity.User;
import com.r2s.core.exception.CustomException;
import com.r2s.core.exception.UnAuthorizedException;
import com.r2s.core.repository.UserRepository;
import com.r2s.core.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceUnitTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthService authService;
//
//    private User user;

    @BeforeEach
    void setUp() {
//        user = User.builder()..build();
    }

    @Test
    void register_success_whenUserIsValid() {
        // Arrange
        RegisterRequest req = RegisterRequest.builder()
                .username("bwocbao")
                .password("123")
                .build();

        when(userRepository.findByUsername("bwocbao"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("123"))
                .thenReturn("encoded123");

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        when(userRepository.save(userCaptor.capture()))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        authService.register(req);

        // Assert user created correctly
        User savedUser = userCaptor.getValue();
        assertEquals("bwocbao", savedUser.getUsername());
        assertEquals("encoded123", savedUser.getPassword());
        assertEquals(Role.ROLE_USER, savedUser.getRole());

        // Verify interactions
        verify(userRepository).findByUsername("bwocbao");
        verify(passwordEncoder).encode("123");
        verify(userRepository).save(any(User.class));
        verifyNoMoreInteractions(userRepository, passwordEncoder);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void register_fail_whenUserIsNotValid() {
        // Arrange
        RegisterRequest req = RegisterRequest.builder()
                .username("bwocbao")
                .password("123")
                .build();

        when(userRepository.findByUsername("bwocbao"))
                .thenReturn(Optional.of(User.builder().username("bwocbao").build()));

        assertThatThrownBy(()->authService.register(req))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Username exist");

        verify(userRepository).findByUsername("bwocbao");
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(jwtUtil, passwordEncoder);
    }

    @Test
    void login_shouldReturnAuthResponse_whenUserIsValid() {
        LoginRequest req= LoginRequest.builder().username("bwocbao").password("123").build();
        when(userRepository.findByUsername("bwocbao")).thenReturn(Optional.of(User.builder()
                .username("bwocbao").password("encoded123").role(Role.ROLE_USER).build()));
        when(passwordEncoder.matches("123", "encoded123")).thenReturn(true);
        when(jwtUtil.generateToken("bwocbao", "ROLE_USER")).thenReturn("token123");

        AuthResponse authResponse= authService.login(req);

        assertEquals("token123", authResponse.getToken());

        verify(userRepository).findByUsername("bwocbao");
        verify(passwordEncoder).matches("123", "encoded123");
        verify(jwtUtil).generateToken("bwocbao", "ROLE_USER");
        verifyNoMoreInteractions(userRepository, passwordEncoder, jwtUtil);
    }

    @Test
    void login_fail_whenUsernameNotExist() {
        LoginRequest req = LoginRequest.builder().username("notExist").password("123").build();
        when(userRepository.findByUsername("notExist")).thenReturn(Optional.empty());

        assertThatThrownBy(()->authService.login(req))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessageContaining("Username does not exist");

        verify(userRepository).findByUsername("notExist");
        verifyNoMoreInteractions(userRepository, passwordEncoder);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void login_fail_whenPasswordNotMatch() {
        LoginRequest req = LoginRequest.builder().username("bwocbao").password("124").build();
        when(userRepository.findByUsername("bwocbao")).thenReturn(Optional.of(User.builder()
                .username("bwocbao").password("encoded123").role(Role.ROLE_USER).build()));
        when(passwordEncoder.matches("124", "encoded123")).thenReturn(false);

        assertThatThrownBy(()->authService.login(req))
                .isInstanceOf(UnAuthorizedException.class)
                .hasMessageContaining("Wrong password");

        verify(userRepository).findByUsername("bwocbao");
        verify(passwordEncoder).matches("124", "encoded123");
        verifyNoMoreInteractions(userRepository, passwordEncoder);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void registerRole_success_whenRoleIsValid() {
        // Arrange
        RegisterRoleRequest req = RegisterRoleRequest.builder()
                .username("admin1")
                .password("123")
                .role("ROLE_ADMIN")
                .build();

        when(userRepository.findByUsername("admin1"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("123"))
                .thenReturn("encoded123");

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(User.class);

        when(userRepository.save(userCaptor.capture()))
                .thenAnswer(inv -> inv.getArgument(0));

        // Act
        authService.registerRole(req);

        // Assert
        User savedUser = userCaptor.getValue();
        assertEquals("admin1", savedUser.getUsername());
        assertEquals("encoded123", savedUser.getPassword());
        assertEquals(Role.ROLE_ADMIN, savedUser.getRole());

        verify(userRepository).findByUsername("admin1");
        verify(passwordEncoder).encode("123");
        verify(userRepository).save(any(User.class));
        verifyNoMoreInteractions(userRepository, passwordEncoder);
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void registerRole_fail_whenUsernameExists() {
        RegisterRoleRequest req = RegisterRoleRequest.builder()
                .username("admin1")
                .password("123")
                .role("admin")
                .build();

        when(userRepository.findByUsername("admin1"))
                .thenReturn(Optional.of(User.builder().username("admin1").build()));

        assertThatThrownBy(() -> authService.registerRole(req))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Username exist");

        verify(userRepository).findByUsername("admin1");
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder, jwtUtil);
    }

    @Test
    void registerRole_fail_whenRoleIsInvalid() {
        RegisterRoleRequest req = RegisterRoleRequest.builder()
                .username("user1")
                .password("123")
                .role("superman") // không tồn tại
                .build();

        when(userRepository.findByUsername("user1"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.registerRole(req))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Invalid role");

        verify(userRepository).findByUsername("user1");
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordEncoder, jwtUtil);
    }
}
