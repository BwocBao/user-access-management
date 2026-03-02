package com.r2s.auth.unit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.auth.controller.AuthController;
import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.dto.RegisterRequest;
import com.r2s.auth.dto.RegisterRoleRequest;
import com.r2s.auth.entity.AuthType;
import com.r2s.auth.service.LoginService;
import com.r2s.auth.service.RegistrationService;
import com.r2s.core.security.JwtFilter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("unittest")
class AuthControllerUnitTest {

    private static final String API="/api/auth";

    private static final String MSG_REGISTER_SUCCESS = "Register success";
    private static final String MSG_LOGIN_SUCCESS = "Login success";
    private static final String MSG_USERNAME_VALID = "Username is required";
    private static final String MSG_PASSWORD_VALID = "Password is required";
    private static final String MSG_ROLE_VALID = "Role is required";
    private static final String MSG_AUTHTYPE_VALID = "Auth type is required";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LoginService loginService;

    @MockBean
    private RegistrationService registrationService;

    @MockBean
    private JwtFilter jwtFilter;
    // =========================
    // HELLO
    // =========================
    @Nested
    class HelloTests {

        @Test
        @WithMockUser(username = "test")
        void shouldReturn200_whenCallHello() throws Exception {
            mockMvc.perform(get(API + "/hello"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Hello from Auth Service"));
        }
    }

    @Nested
    class RegisterTests {
        @Test
        void register_shouldReturnOk_whenRequestIsValid() throws Exception {
            RegisterRequest req = RegisterRequest.builder()
                    .username("bwocbao")
                    .password("123")
                    .build();

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message")
                            .value(MSG_REGISTER_SUCCESS))
                    .andExpect(jsonPath("$.data").isEmpty());

            verify(registrationService).register(any(RegisterRequest.class));
        }

        @Test
        void register_shouldReturnBadRequest_whenRequestIsInvalid() throws Exception {
            RegisterRequest req = RegisterRequest.builder()
                    .username("")   // invalid
                    .password("")
                    .build();

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.username").value(MSG_USERNAME_VALID))
                    .andExpect(jsonPath("$.password").value(MSG_PASSWORD_VALID));

            verifyNoInteractions(registrationService);
        }
    }

    @Nested
    class LoginTests {

        @Test
        void login_shouldReturnToken_whenValidRequest() throws Exception {

            LoginRequest req = LoginRequest.builder()
                    .username("bwocbao")
                    .password("123")
                    .type(AuthType.USERNAME_PASSWORD)
                    .build();

            AuthResponse response = new AuthResponse("token123");

            when(loginService.login(any(LoginRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post(API + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value(MSG_LOGIN_SUCCESS))
                    .andExpect(jsonPath("$.data.token").value("token123"));

            verify(loginService).login(any(LoginRequest.class));
        }

        @Test
        void login_shouldReturnBadRequest_whenInvalidRequest() throws Exception {

            LoginRequest req = LoginRequest.builder()
                    .username("")
                    .password("")
                    .type(null)
                    .build();

            mockMvc.perform(post(API + "/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.username").value(MSG_USERNAME_VALID))
                    .andExpect(jsonPath("$.password").value(MSG_PASSWORD_VALID))
                    .andExpect(jsonPath("$.type").value(MSG_AUTHTYPE_VALID));

            verifyNoInteractions(loginService);
        }
    }

    @Nested
    class RegisterRoleTests {

        @Test
        void registerRole_shouldReturnOk_whenValidRequest() throws Exception {

            RegisterRoleRequest req = RegisterRoleRequest.builder()
                    .username("admin1")
                    .password("123")
                    .role("ROLE_ADMIN")
                    .build();

            mockMvc.perform(post(API + "/register/role")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message")
                            .value(MSG_REGISTER_SUCCESS));

            verify(registrationService)
                    .registerWithRole(any(RegisterRoleRequest.class));
        }

        @Test
        void registerRole_shouldReturnBadRequest_whenInvalidRequest() throws Exception {

            RegisterRoleRequest req = RegisterRoleRequest.builder()
                    .username("")
                    .password("")
                    .role("")
                    .build();

            mockMvc.perform(post(API + "/register/role")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.username").value(MSG_USERNAME_VALID))
                    .andExpect(jsonPath("$.password").value(MSG_PASSWORD_VALID));

            verifyNoInteractions(registrationService);
        }
    }
}

//@WebMvcTest(controllers = AuthController.class)
//@AutoConfigureMockMvc(addFilters = false)
//@ActiveProfiles("unittest")
//class AuthControllerUnitTest {
//    private static final String API="/api/auth";
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    @MockBean
//    private AuthService authService;
//
//
//    @MockBean
//    private JwtFilter jwtFilter;
//
//    @Test
//    void hello_shouldReturnHelloMessage() throws Exception {
//        mockMvc.perform(get("/api/auth/hello"))
//                .andExpect(status().isOk())
//                .andExpect(content().string("Hello from Auth Service"));
//    }
//
//    @Test
//    void register_shouldReturnOk_whenRequestIsValid() throws Exception {
//        RegisterRequest req = RegisterRequest.builder()
//                .username("bwocbao")
//                .password("123")
//                .build();
//
//        mockMvc.perform(post("/api/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(req)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.status").value(200))
//                .andExpect(jsonPath("$.message")
//                        .value("User registered successfully"))
//                .andExpect(jsonPath("$.data").isEmpty());
//
//        verify(authService).register(any(RegisterRequest.class));
//    }
//
//    @Test
//    void register_shouldReturnBadRequest_whenRequestIsInvalid() throws Exception {
//        RegisterRequest req = RegisterRequest.builder()
//                .username("")   // invalid
//                .password("")
//                .build();
//
//        mockMvc.perform(post("/api/auth/register")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(req)))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.username").value("Username is required"))
//                .andExpect(jsonPath("$.password").value("Password is required"));
//
//        verifyNoInteractions(authService);
//    }
//
//    // ================= LOGIN =================
//
//    @Test
//    void login_shouldReturnToken_whenCredentialsAreValid() throws Exception {
//        LoginRequest req = LoginRequest.builder()
//                .username("bwocbao")
//                .password("123")
//                .build();
//
//        when(authService.login(any(LoginRequest.class)))
//                .thenReturn(new AuthResponse("token123"));
//
//        mockMvc.perform(post("/api/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(req)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.status").value(200))
//                .andExpect(jsonPath("$.message")
//                        .value("User logged successfully"))
//                .andExpect(jsonPath("$.data.token").value("token123"));
//
//        verify(authService).login(any(LoginRequest.class));
//    }
//
//    @Test
//    void login_shouldReturnBadRequest_whenRequestIsInvalid() throws Exception {
//        LoginRequest req = LoginRequest.builder()
//                .username("")
//                .password("")
//                .build();
//
//        mockMvc.perform(post("/api/auth/login")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(req)))
//                .andExpect(status().isBadRequest());
//
//        verifyNoInteractions(authService);
//    }
//
//    // ================= REGISTER ROLE =================
//
//    @Test
//    void registerRole_shouldReturnOk_whenRequestIsValid() throws Exception {
//        RegisterRoleRequest req = RegisterRoleRequest.builder()
//                .username("admin1")
//                .password("123")
//                .role("ROLE_ADMIN")
//                .build();
//
//        mockMvc.perform(post("/api/auth/register/role")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(req)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.status").value(200))
//                .andExpect(jsonPath("$.message").value("User registered successfully"))
//                .andExpect(jsonPath("$.data").isEmpty());
//
//        verify(authService).registerRole(any(RegisterRoleRequest.class));
//    }
//
//    @Test
//    void registerRole_shouldReturnBadRequest_whenRequestIsInvalid() throws Exception {
//        RegisterRoleRequest req = RegisterRoleRequest.builder()
//                .username("")
//                .password("")
//                .role("")
//                .build();
//
//        mockMvc.perform(post("/api/auth/register/role")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(req)))
//                .andExpect(status().isBadRequest())
//                .andExpect(jsonPath("$.username").value("Username is required"))
//                .andExpect(jsonPath("$.password").value("Password is required"))
//                        .andExpect(jsonPath("$.role").value("Role is required"));
//
//
//        verifyNoInteractions(authService);
//    }
