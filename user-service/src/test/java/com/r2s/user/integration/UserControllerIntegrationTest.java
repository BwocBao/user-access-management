package com.r2s.user.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.dto.RegisterRoleRequest;
import com.r2s.core.entity.Role;
import com.r2s.core.entity.User;
import com.r2s.core.repository.UserRepository;
import com.r2s.core.security.JwtUtil;
import com.r2s.user.dto.UpdateUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class UserControllerIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JwtUtil jwtUtil;

    @Autowired
    UserRepository userRepository;

    //  Trước khi chạy MỖI test case → xóa sạch dữ liệu trong DB. Vẫn tái sử dụng docker container
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");
    }

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("test_db")
                    .withUsername("postgres")
                    .withPassword("123456");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
    }

    private String generateToken(String username, String role) {

        return jwtUtil.generateToken(username, role);
    }


    @Test
    void hello_shouldReturnHello() throws Exception {

        mockMvc.perform(get("/api/users/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello from User Service"));
    }

    @Test
    void getAllUsers_shouldReturn200_whenAdmin() throws Exception {


        String token = generateToken("admin", "ROLE_ADMIN");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Users retrieved successfully"))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getAllUsers_shouldReturn403_whenUser() throws Exception {


        String token = generateToken("user", "ROLE_USer");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))

                .andExpect(status().isForbidden());
    }

    @Test
    void getMe_shouldReturn200AndProfile() throws Exception {

        String token = generateToken("user", "ROLE_USER");

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Profile retrieved successfully"))
                .andExpect(jsonPath("$.data.username").value("user"))
                .andExpect(jsonPath("$.data.role").value("ROLE_USER"));
    }

    @Test
    void updateMe_shouldReturn200AndProfile() throws Exception {

        String token = generateToken("user", "ROLE_USER");

        UpdateUserRequest req = new UpdateUserRequest();
        req.setFullName("User Updated");
        req.setEmail("user@gmail.com");

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Profile updated successfully"))
                .andExpect(jsonPath("$.data.username").value("user"))
                .andExpect(jsonPath("$.data.fullName").value("User Updated"))
                .andExpect(jsonPath("$.data.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.data.email").value("user@gmail.com"));

    }

    @Test
    void deleteUser_shouldReturn200_whenAdmin() throws Exception {

        // Tạo user trước
        User user = User.builder().username("user").password("123456").role(Role.ROLE_USER).build();

        userRepository.save(user);

        // Login admin
        String adminToken = generateToken("admin", "ROLE_ADMIN");

        mockMvc.perform(delete("/api/users/user")
                        .header("Authorization", "Bearer " + adminToken))

                .andExpect(status().isNoContent());
    }


    @Test
    void deleteUser_shouldReturn403_whenNotAdmin() throws Exception {

        // Tạo user trước
        User user = User.builder().username("user").password("123456").role(Role.ROLE_USER).build();

        userRepository.save(user);

        // Login admin
        String token = generateToken("moderator", "ROLE_MODERATOR");

        mockMvc.perform(delete("/api/users/user")
                        .header("Authorization", "Bearer " + token))

                .andExpect(status().isForbidden());
    }
}