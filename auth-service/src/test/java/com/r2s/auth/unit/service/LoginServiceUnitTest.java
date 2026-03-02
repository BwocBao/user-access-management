package com.r2s.auth.unit.service;

import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.entity.AuthType;
import com.r2s.auth.service.LoginServiceImpl;
import com.r2s.auth.strategy.LoginStrategy;
import com.r2s.core.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginServiceUnitTest {

    @Mock
    private LoginStrategy strategy1;

    @Mock
    private LoginStrategy strategy2;

    @InjectMocks
    private LoginServiceImpl loginService;

    @BeforeEach
    void setUp() {
        loginService = new LoginServiceImpl(List.of(strategy1, strategy2));
    }

    @Test
    void login_success_whenStrategySupportsType() {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .type(AuthType.USERNAME_PASSWORD)
                .build();

        AuthResponse expectedResponse = new AuthResponse("token123");

        when(strategy1.supports(AuthType.USERNAME_PASSWORD)).thenReturn(false);
        when(strategy2.supports(AuthType.USERNAME_PASSWORD)).thenReturn(true);
        when(strategy2.authenticate(request)).thenReturn(expectedResponse);

        // Act
        AuthResponse response = loginService.login(request);

        // Assert
        assertEquals("token123", response.getToken());

        verify(strategy1).supports(AuthType.USERNAME_PASSWORD);
        verify(strategy2).supports(AuthType.USERNAME_PASSWORD);
        verify(strategy2).authenticate(request);
        verifyNoMoreInteractions(strategy1, strategy2);
    }

    @Test
    void login_fail_whenNoStrategySupportsType() {
        // Arrange
        LoginRequest request = LoginRequest.builder()
                .type(AuthType.USERNAME_PASSWORD)
                .build();

        when(strategy1.supports(any())).thenReturn(false);
        when(strategy2.supports(any())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> loginService.login(request))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Authentication type not supported");

        verify(strategy1).supports(AuthType.USERNAME_PASSWORD);
        verify(strategy2).supports(AuthType.USERNAME_PASSWORD);
        verifyNoMoreInteractions(strategy1, strategy2);
    }
}
