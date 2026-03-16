package com.r2s.auth.unit.controller;



import com.r2s.auth.config.SecurityConfig;
import com.r2s.auth.controller.InternalAuthController;
import com.r2s.auth.entity.User;
import com.r2s.auth.repository.UserRepository;
import com.r2s.core.security.JwtFilter;
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
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalAuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({SecurityConfig.class, InternalAuthControllerTest.TestSecurityConfig.class})
class InternalAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

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
    void shouldDeleteUser_whenUserExists() throws Exception {

        User user = new User();
        user.setUsername("beo9");

        when(userRepository.findByUsername("beo9"))
                .thenReturn(Optional.of(user));

        mockMvc.perform(delete("/internal/users/beo9").with(csrf()))
                .andExpect(status().isOk());

        verify(userRepository).delete(user);
    }

    @Test
    @WithMockUser(roles = "SERVICE")
    void shouldReturnOk_whenUserNotExists() throws Exception {

        when(userRepository.findByUsername("beo9"))
                .thenReturn(Optional.empty());

        mockMvc.perform(delete("/internal/users/beo9").with(csrf()))
                .andExpect(status().isOk());

        verify(userRepository, never()).delete(any());
    }

    @Test
    @WithMockUser(roles = "USER")
    void shouldReturnForbidden_whenNotServiceRole() throws Exception {

        mockMvc.perform(delete("/internal/users/beo9").with(csrf()))
                .andExpect(status().isForbidden());

        verify(userRepository, never()).delete(any());
    }
}