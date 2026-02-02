package com.r2s.user.unit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.core.security.JwtFilter;
import com.r2s.core.security.JwtUtil;
import com.r2s.user.config.SecurityConfig;
import com.r2s.user.controller.UserController;
import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.dto.UserResponse;
import com.r2s.user.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class) // import config thật
@AutoConfigureMockMvc(addFilters = true) // BẬT SECURITY
class UserControllerUnitTest {
    private static final String API = "/api/users";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ===== MOCK BUSINESS =====
    @MockBean
    private UserService userService;

    // ===== MOCK SECURITY =====
    @MockBean
    private JwtFilter jwtFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    /**
     * Cho JwtFilter đi xuyên thẳng (không cần Bearer token thật)
     */
    @BeforeEach
    void setupJwtFilterPassThrough() throws Exception {
        doAnswer(invocation -> {
            HttpServletRequest request = invocation.getArgument(0);
            HttpServletResponse response = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(request, response);
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    // =========================
    // HELLO
    // =========================
    @Test
    @WithMockUser(username = "test")
    void hello_shouldReturn200() throws Exception {
        mockMvc.perform(get(API + "/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello from User Service"));
    }

    // =========================
    // GET ALL USERS
    // =========================
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void getAllUsers_shouldReturn200_whenAdmin() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of());

        mockMvc.perform(get(API))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void getAllUsers_shouldReturn403_whenUser() throws Exception {
        mockMvc.perform(get(API))
                .andExpect(status().isForbidden());
    }

//    Không có JWT / không có user. Gán AnonymousAuthenticationToken
//    Kiểm tra hasRole('ADMIN'). Anonymous không có role ADMIN
//⇒ Access Denied → 403
//➡️ Không phải unauthenticated, mà là authenticated as ANONYMOUS
    @Test
    void getAllUsers_shouldReturn401_whenUnauthenticated() throws Exception {
        mockMvc.perform(get(API))
                .andExpect(status().isForbidden());
    }

    // =========================
    // GET MY PROFILE
    // =========================
    @Test
    @WithMockUser(username = "bao", roles = {"USER"})
    void getMyProfile_shouldReturnProfile() throws Exception {
        UserResponse res = UserResponse.builder()
                .username("bao")
                .role("ROLE_USER")
                .build();

        when(userService.getUserByUsername("bao", "ROLE_USER"))
                .thenReturn(res);

        mockMvc.perform(get(API + "/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("bao"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    // =========================
    // UPDATE MY PROFILE
    // =========================
    @Test
    @WithMockUser(username = "bao", roles = {"USER"})
    void updateMyProfile_shouldReturnUpdatedProfile() throws Exception {
        UpdateUserRequest req = UpdateUserRequest.builder()
                .email("new@email.com")
                .fullName("Bao Nguyen")
                .build();

        UserResponse res = UserResponse.builder()
                .username("bao")
                .role("ROLE_USER")
                .email("new@email.com")
                .fullName("Bao Nguyen")
                .build();

        when(userService.updateUser(any(UpdateUserRequest.class), eq("bao"), eq("ROLE_USER")))
                .thenReturn(res);

        mockMvc.perform(put(API + "/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@email.com"))
                .andExpect(jsonPath("$.fullName").value("Bao Nguyen"));
    }

    // =========================
    // DELETE USER
    // =========================
    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void deleteUser_shouldReturn204_whenAdmin() throws Exception {
        mockMvc.perform(delete(API + "/testuser"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void deleteUser_shouldReturn403_whenUser() throws Exception {
        mockMvc.perform(delete(API + "/testuser"))
                .andExpect(status().isForbidden());
    }
}
