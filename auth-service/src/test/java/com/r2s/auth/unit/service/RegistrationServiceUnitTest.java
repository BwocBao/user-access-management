package com.r2s.auth.unit.service;

import com.r2s.auth.dto.RegisterRequest;
import com.r2s.auth.dto.RegisterRoleRequest;
import com.r2s.auth.service.PasswordService;
import com.r2s.auth.service.RegistrationServiceImpl;
import com.r2s.core.entity.Role;
import com.r2s.core.entity.User;
import com.r2s.core.exception.CustomException;
import com.r2s.core.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceUnitTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordService passwordService;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    @Nested
    class RegisterTest {

        @Test
        void register_success_whenUserIsValid() {
            RegisterRequest req = buildRegisterRequest("bwocbao", "123");

            when(userRepository.existsByUsername("bwocbao"))
                    .thenReturn(false);

            when(passwordService.encode("123"))
                    .thenReturn("encoded123");

            ArgumentCaptor<User> userCaptor =
                    ArgumentCaptor.forClass(User.class);

            when(userRepository.save(userCaptor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));

            registrationService.register(req);

            User savedUser = userCaptor.getValue();
            assertEquals("bwocbao", savedUser.getUsername());
            assertEquals("encoded123", savedUser.getPassword());
            assertEquals(Role.ROLE_USER, savedUser.getRole());

            verify(userRepository).existsByUsername("bwocbao");
            verify(passwordService).encode("123");
            verify(userRepository).save(any(User.class));
            verifyNoMoreInteractions(userRepository, passwordService);
        }

        @Test
        void register_fail_whenUserExists() {
            RegisterRequest req = buildRegisterRequest("bwocbao", "123");

            when(userRepository.existsByUsername("bwocbao"))
                    .thenReturn(true);

            assertThatThrownBy(() -> registrationService.register(req))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Username already exists");

            verify(userRepository).existsByUsername("bwocbao");
            verifyNoMoreInteractions(userRepository, passwordService);
        }
    }

    @Nested
    class RegisterWithRoleTest {

        @Test
        void registerRole_success_whenRoleIsValid() {
            RegisterRoleRequest req =
                    buildRegisterRoleRequest("admin1", "123", "ROLE_ADMIN");

            when(userRepository.existsByUsername("admin1"))
                    .thenReturn(false);

            when(passwordService.encode("123"))
                    .thenReturn("encoded123");

            ArgumentCaptor<User> userCaptor =
                    ArgumentCaptor.forClass(User.class);

            when(userRepository.save(userCaptor.capture()))
                    .thenAnswer(inv -> inv.getArgument(0));

            registrationService.registerWithRole(req);

            User savedUser = userCaptor.getValue();
            assertEquals("admin1", savedUser.getUsername());
            assertEquals("encoded123", savedUser.getPassword());
            assertEquals(Role.ROLE_ADMIN, savedUser.getRole());

            verify(userRepository).existsByUsername("admin1");
            verify(passwordService).encode("123");
            verify(userRepository).save(any(User.class));
            verifyNoMoreInteractions(userRepository, passwordService);
        }

        @Test
        void registerRole_fail_whenUsernameExists() {
            RegisterRoleRequest req =
                    buildRegisterRoleRequest("admin1", "123", "ROLE_ADMIN");

            when(userRepository.existsByUsername("admin1"))
                    .thenReturn(true);

            assertThatThrownBy(() -> registrationService.registerWithRole(req))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Username already exists");

            verify(userRepository).existsByUsername("admin1");
            verifyNoMoreInteractions(userRepository, passwordService);
        }

        @Test
        void registerRole_fail_whenRoleIsInvalid() {
            RegisterRoleRequest req =
                    buildRegisterRoleRequest("user1", "123", "superman");

            when(userRepository.existsByUsername("user1"))
                    .thenReturn(false);

            assertThatThrownBy(() -> registrationService.registerWithRole(req))
                    .isInstanceOf(CustomException.class)
                    .hasMessageContaining("Invalid role");

            verify(userRepository).existsByUsername("user1");
            verifyNoMoreInteractions(userRepository, passwordService);
        }
    }

    private RegisterRequest buildRegisterRequest(String username, String password) {
        return RegisterRequest.builder()
                .username(username)
                .password(password)
                .build();
    }

    private RegisterRoleRequest buildRegisterRoleRequest(String username, String password, String role) {
        return RegisterRoleRequest.builder()
                .username(username)
                .password(password)
                .role(role)
                .build();
    }
}
