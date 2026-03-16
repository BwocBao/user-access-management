package com.r2s.user.unit.controller;

import com.r2s.core.security.JwtFilter;
import com.r2s.core.security.JwtUtil;
import com.r2s.user.config.SecurityConfig;
import com.r2s.user.controller.InternalUserController;
import com.r2s.user.entity.UserProfile;
import com.r2s.user.repository.UserProfileRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InternalUserController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, InternalUserControllerTest.TestSecurityConfig.class})
class InternalUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserProfileRepository userRepository;


    // KHÔNG dùng @MockBean JwtFilter ở đây nữa do Request -> JwtFilter (mock)
    // -> KHÔNG gọi filterChain.doFilter() -> Request bị dừng

    @TestConfiguration
    @EnableMethodSecurity
    static class TestSecurityConfig {
        // Tạo Dummy Filter để không làm đứt chuỗi request
        @Bean
        public JwtFilter jwtFilter() {
            return new JwtFilter(null) { // Truyền null hoặc các dependencies cần thiết
                @Override
                protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
                    filterChain.doFilter(request, response);
                }
            };
        }
    }

    @Test
    @WithMockUser(roles = "SERVICE")
    void shouldCreateUser_whenUserNotExists() throws Exception {

        mockMvc.perform(post("/internal/auth/sync/beo9"))
                .andExpect(status().isOk());

        verify(userRepository).save(any(UserProfile.class));
    }
    @Test
    @WithMockUser(roles = "SERVICE")
    void shouldIgnoreDuplicateUser_whenUniqueConstraintViolation() throws Exception {

        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(userRepository).save(any(UserProfile.class));

        mockMvc.perform(post("/internal/auth/sync/beo9"))
                .andExpect(status().isOk());

        verify(userRepository).save(any(UserProfile.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturnForbidden_whenNotServiceRole() throws Exception {

        mockMvc.perform(post("/internal/auth/sync/beo9"))
                .andExpect(status().isForbidden());

        verify(userRepository, never()).save(any());
    }
}