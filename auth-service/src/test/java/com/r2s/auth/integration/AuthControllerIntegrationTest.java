package com.r2s.auth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.dto.RegisterRequest;
import com.r2s.auth.dto.RegisterRoleRequest;
import com.r2s.auth.messaging.OutboxService;
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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
        jdbcTemplate.execute("TRUNCATE TABLE users RESTART IDENTITY CASCADE");
    }

    @BeforeEach
    void setupMocks() {
        doNothing().when(outboxService).saveUserRegisteredEvent(anyString());
    }

    @Test
    void hello_shouldReturnHello() throws Exception {
        mockMvc.perform(get("/api/auth/hello"))
                .andExpect(status().isOk())
                .andExpect(content().string("Hello from Auth Service"));

        verifyNoInteractions(outboxService);
    }

    @Test
    void register_shouldReturn200_whenValid() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("bwocbao");
        request.setPassword("123456");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("User registered successfully"));

        verify(outboxService).saveUserRegisteredEvent("bwocbao");
    }

    @Test
    void register_shouldReturn400_whenInvalid() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("");
        request.setPassword("");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(outboxService);
    }

    @Test
    void register_shouldReturn400_whenUsernameAlreadyExists() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("bwocbao");
        request.setPassword("123456");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Username exist"));

        verify(outboxService, times(1)).saveUserRegisteredEvent("bwocbao");
    }

    @Test
    void registerRole_shouldReturn200_whenValid() throws Exception {
        RegisterRoleRequest request = new RegisterRoleRequest();
        request.setUsername("admin1");
        request.setPassword("123456");
        request.setRole("ROLE_ADMIN");

        mockMvc.perform(post("/api/auth/register/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("User registered successfully"));

        verify(outboxService).saveUserRegisteredEvent("admin1");
    }

    @Test
    void registerRole_shouldReturn400_whenInvalidRole() throws Exception {
        RegisterRoleRequest request = new RegisterRoleRequest();
        request.setUsername("bwocbao");
        request.setPassword("123456");
        request.setRole("Role_superman");

        mockMvc.perform(post("/api/auth/register/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid role"));

        verifyNoInteractions(outboxService);
    }

    @Test
    void registerRole_shouldReturn400_whenUsernameAlreadyExists() throws Exception {
        RegisterRoleRequest request = new RegisterRoleRequest();
        request.setUsername("bwocbao");
        request.setPassword("123456");
        request.setRole("ROLE_ADMIN");

        mockMvc.perform(post("/api/auth/register/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/register/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Username exist"));

        verify(outboxService, times(1)).saveUserRegisteredEvent("bwocbao");
    }

    @Test
    void login_shouldReturn200_whenValid() throws Exception {
        RegisterRequest register = new RegisterRequest();
        register.setUsername("bwocbao");
        register.setPassword("123456");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isOk());

        LoginRequest login = new LoginRequest();
        login.setUsername("bwocbao");
        login.setPassword("123456");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("User logged successfully"))
                .andExpect(jsonPath("$.data.token").exists());

        verify(outboxService).saveUserRegisteredEvent("bwocbao");
    }

    @Test
    void login_shouldReturn401_whenUsernameDoesNotExist() throws Exception {
        LoginRequest login = new LoginRequest();
        login.setUsername("bwocbao");
        login.setPassword("123456");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Username does not exist"));

        verifyNoInteractions(outboxService);
    }

    @Test
    void login_shouldReturn401_whenWrongPassword() throws Exception {
        RegisterRequest register = new RegisterRequest();
        register.setUsername("bwocbao");
        register.setPassword("123456");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isOk());

        LoginRequest login = new LoginRequest();
        login.setUsername("bwocbao");
        login.setPassword("123457");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Wrong password"));

        verify(outboxService).saveUserRegisteredEvent("bwocbao");
    }
}