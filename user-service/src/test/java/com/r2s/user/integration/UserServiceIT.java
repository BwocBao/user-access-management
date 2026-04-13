package com.r2s.user.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.core.security.JwtUtil;
import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.entity.UserProfile;
import com.r2s.user.messaging.OutboxService;
import com.r2s.user.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class UserServiceIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserProfileRepository userRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private OutboxService outboxService;

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

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE user_profile RESTART IDENTITY CASCADE");
    }

    @BeforeEach
    void setupMocks() {
        doNothing().when(outboxService).saveUserDeletedEvent(anyString());
    }

    private String generateToken(String username, String role) {
        return jwtUtil.generateToken(username, role);
    }

    @Test
    void hello_shouldReturnHello() throws Exception {
        mockMvc.perform(get("/api/users/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello from User Service"));

        verifyNoInteractions(outboxService);
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

        verifyNoInteractions(outboxService);
    }

    @Test
    void getAllUsers_shouldReturn403_whenUser() throws Exception {
        String token = generateToken("user", "ROLE_USER");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        verifyNoInteractions(outboxService);
    }

    @Test
    void getMyProfile_shouldReturn200_whenProfileExists() throws Exception {
        userRepository.save(
                UserProfile.builder()
                        .username("user")
                        .fullName("User Full Name")
                        .email("user@gmail.com")
                        .build()
        );

        String token = generateToken("user", "ROLE_USER");

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Profile retrieved successfully"))
                .andExpect(jsonPath("$.data.username").value("user"))
                .andExpect(jsonPath("$.data.fullName").value("User Full Name"))
                .andExpect(jsonPath("$.data.email").value("user@gmail.com"));

        verifyNoInteractions(outboxService);
    }

    @Test
    void getMyProfile_shouldReturn404_whenProfileNotFound() throws Exception {
        String token = generateToken("user", "ROLE_USER");

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found: user"));

        verifyNoInteractions(outboxService);
    }

    @Test
    void updateMyProfile_shouldReturn200_whenValid() throws Exception {
        userRepository.save(
                UserProfile.builder()
                        .username("user")
                        .fullName("")
                        .email("")
                        .build()
        );

        String token = generateToken("user", "ROLE_USER");

        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("User Updated");
        request.setEmail("user@gmail.com");

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Profile updated successfully"))
                .andExpect(jsonPath("$.data.username").value("user"))
                .andExpect(jsonPath("$.data.fullName").value("User Updated"))
                .andExpect(jsonPath("$.data.email").value("user@gmail.com"));

        verifyNoInteractions(outboxService);
    }

    @Test
    void updateMyProfile_shouldReturn404_whenProfileNotFound() throws Exception {
        String token = generateToken("user", "ROLE_USER");

        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("User Updated");
        request.setEmail("user@gmail.com");

        mockMvc.perform(put("/api/users/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found: user"));

        verifyNoInteractions(outboxService);
    }

    @Test
    void deleteUser_shouldReturn204_whenAdminAndUserExists() throws Exception {
        userRepository.save(
                UserProfile.builder()
                        .username("user")
                        .fullName("User")
                        .email("user@gmail.com")
                        .build()
        );

        String adminToken = generateToken("admin", "ROLE_ADMIN");

        mockMvc.perform(delete("/api/users/user")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        verify(outboxService).saveUserDeletedEvent("user");
    }

    @Test
    void deleteUser_shouldReturn404_whenUserNotFound() throws Exception {
        String adminToken = generateToken("admin", "ROLE_ADMIN");

        mockMvc.perform(delete("/api/users/user")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("User not found: user"));

        verifyNoInteractions(outboxService);
    }

    @Test
    void deleteUser_shouldReturn403_whenNotAdmin() throws Exception {
        userRepository.save(
                UserProfile.builder()
                        .username("user")
                        .fullName("User")
                        .email("user@gmail.com")
                        .build()
        );

        String token = generateToken("moderator", "ROLE_MODERATOR");

        mockMvc.perform(delete("/api/users/user")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        verifyNoInteractions(outboxService);
    }

    @Test
    void getMyProfile_shouldReturn401_whenNoToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(outboxService);
    }

    @Test
    void updateMyProfile_shouldReturn401_whenNoToken() throws Exception {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setFullName("User Updated");
        request.setEmail("user@gmail.com");

        mockMvc.perform(put("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(outboxService);
    }

    @Test
    void deleteUser_shouldReturn401_whenNoToken() throws Exception {
        mockMvc.perform(delete("/api/users/user"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(outboxService);
    }
}