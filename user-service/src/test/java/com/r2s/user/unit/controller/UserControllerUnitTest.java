package com.r2s.user.unit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.core.security.JwtFilter;
import com.r2s.core.security.JwtUtil;
import com.r2s.user.config.SecurityConfig;
import com.r2s.user.controller.UserController;
import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.dto.UserResponse;
import com.r2s.user.service.UserManagementService;
import com.r2s.user.service.UserProfileService;
import com.r2s.user.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@AutoConfigureMockMvc
@ActiveProfiles("unittest")
class UserControllerUnitTest {

    private static final String API = "/api/users";

    private static final String USERNAME_BAO = "bao";
    private static final String ROLE_USER = "ROLE_USER";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private static final String MSG_USERS_RETRIEVED = "Users retrieved successfully";
    private static final String MSG_PROFILE_RETRIEVED = "Profile retrieved successfully";
    private static final String MSG_PROFILE_UPDATED = "Profile updated successfully";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserManagementService userManagementService;

    @MockBean
    private UserProfileService userProfileService;

    @MockBean
    private JwtFilter jwtFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private PasswordEncoder passwordEncoder;

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
    @Nested
    class HelloTests {

        @Test
        @WithMockUser(username = "test")
        void shouldReturn200_whenCallHello() throws Exception {
            mockMvc.perform(get(API + "/hello"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Hello from User Service"));
        }
    }

    // =========================
    // GET ALL USERS
    // =========================
    @Nested
    class GetAllUsersTests {

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldReturn200_whenAdminRequestsUsers() throws Exception {

            List<UserResponse> users = List.of(
                    new UserResponse("admin2", ROLE_ADMIN, "admin@example.com", "Gia Bao"),
                    new UserResponse("jane", ROLE_USER, "jane@example.com", "Jane Smith")
            );

            when(userManagementService.getAllUsers()).thenReturn(users);

            mockMvc.perform(get(API))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value(MSG_USERS_RETRIEVED))
                    .andExpect(jsonPath("$.data.length()").value(2));

            verify(userManagementService).getAllUsers();
        }

        @Test
        @WithMockUser(username = "user", roles = {"USER"})
        void shouldReturn403_whenUserRequestsUsers() throws Exception {
            mockMvc.perform(get(API))
                    .andExpect(status().isForbidden());
        }

        @Test
        void shouldReturn403_whenUnauthenticated() throws Exception {
            mockMvc.perform(get(API))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================
    // PROFILE
    // =========================
    @Nested
    class ProfileTests {

        @Test
        @WithMockUser(username = USERNAME_BAO, roles = {"USER"})
        void shouldReturnProfile_whenAuthenticated() throws Exception {

            UserResponse response = buildUserResponse();
            when(userProfileService.getUser(USERNAME_BAO)).thenReturn(response);

            mockMvc.perform(get(API + "/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value(MSG_PROFILE_RETRIEVED))
                    .andExpect(jsonPath("$.data.username").value(USERNAME_BAO));

            verify(userProfileService).getUser(USERNAME_BAO);
        }

        @Test
        @WithMockUser(username = USERNAME_BAO, roles = {"USER"})
        void shouldUpdateProfile_whenValidRequest() throws Exception {

            UpdateUserRequest request = UpdateUserRequest.builder()
                    .email("new@email.com")
                    .fullName("Bao Nguyen")
                    .build();

            UserResponse response = buildUserResponse();
            when(userProfileService.updateUser(any())).thenReturn(response);

            mockMvc.perform(put(API + "/me")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(MSG_PROFILE_UPDATED));

            verify(userProfileService).updateUser(any());
        }

        @Test
        void shouldReturn403_whenUnauthenticated() throws Exception {
            mockMvc.perform(get(API + "/me"))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================
    // DELETE USER
    // =========================
    @Nested
    class DeleteUserTests {

        @Test
        @WithMockUser(username = "admin", roles = {"ADMIN"})
        void shouldReturn204_whenAdminDeletesUser() throws Exception {

            mockMvc.perform(delete(API + "/testuser"))
                    .andExpect(status().isNoContent());

            verify(userManagementService).deleteUser("testuser");
        }

        @Test
        @WithMockUser(username = "user", roles = {"USER"})
        void shouldReturn403_whenUserDeletesUser() throws Exception {
            mockMvc.perform(delete(API + "/testuser"))
                    .andExpect(status().isForbidden());
        }
    }

    // =========================
    // HELPER
    // =========================
    private UserResponse buildUserResponse() {
        return UserResponse.builder()
                .username(USERNAME_BAO)
                .role(ROLE_USER)
                .email("bao@email.com")
                .fullName("Bao Nguyen")
                .build();
    }
}

//@WebMvcTest(UserController.class)
//@Import(SecurityConfig.class) // import config thật
//@AutoConfigureMockMvc // BẬT SECURITY / mặc định addFilters = true
//@ActiveProfiles("unittest")
//class UserControllerUnitTest {
//    private static final String API = "/api/users";
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @Autowired
//    private ObjectMapper objectMapper;
//
//    // ===== MOCK BUSINESS =====
////    @MockBean
////    private UserService userService;
//
//    @MockBean
//    private UserManagementService userManagementService;
//
//    @MockBean
//    private UserProfileService userProfileService;
//
//    // ===== MOCK SECURITY =====
//    @MockBean
//    private JwtFilter jwtFilter;
//
//    @MockBean
//    private JwtUtil jwtUtil;
//
//    @MockBean
//    private UserDetailsService userDetailsService;
//
//    @MockBean
//    private PasswordEncoder passwordEncoder;
//
//    /**
//     * Cho JwtFilter đi xuyên thẳng (không cần Bearer token thật)
//     */
//    @BeforeEach
//    void setupJwtFilterPassThrough() throws Exception {
//        doAnswer(invocation -> {
//            HttpServletRequest request = invocation.getArgument(0);
//            HttpServletResponse response = invocation.getArgument(1);
//            FilterChain chain = invocation.getArgument(2);
//            chain.doFilter(request, response);
//            return null;
//        }).when(jwtFilter).doFilter(any(), any(), any());
//    }
//
//    private UserResponse buildUserResponse() {
//        return UserResponse.builder()
//                .username("bao")
//                .role("ROLE_USER")
//                .email("bao@email.com")
//                .fullName("Bao Nguyen")
//                .build();
//    }
//
//    // =========================
//    // HELLO
//    // =========================
//    @Nested
//    class HelloTests {
//
//        @Test
//        @WithMockUser(username = "test")
//        void shouldReturn200_whenCallHello() throws Exception {
//            mockMvc.perform(get(API + "/hello"))
//                    .andExpect(status().isOk())
//                    .andExpect(content().string("Hello from User Service"));
//        }
//    }
//
//    // =========================
//    // GET ALL USERS
//    // =========================
//    @Test
//    @WithMockUser(username = "admin", roles = {"ADMIN"})
//    void getAllUsers_shouldReturn200_whenAdmin() throws Exception {
//        List<UserResponse> mockUsers = List.of(
//                new UserResponse("admin2", "ROLE_ADMIN", "admin@example.com", "Gia Bao"),
//                new UserResponse("jane", "ROLE_USER", "jane@example.com", "Jane Smith")
//        );
//
//        when(userManagementService.getAllUsers()).thenReturn(mockUsers);
//
//
//        ResultActions response = mockMvc.perform(get(API));
//        response.andExpect(status().isOk())
//                .andExpect(jsonPath("$.status").value(200))
//                .andExpect(jsonPath("$.message")
//                        .value("Users retrieved successfully"))
//                // Check size
//                .andExpect(jsonPath("$.data").isArray())
//                .andExpect(jsonPath("$.data.length()").value(2))
//
//                // User 1
//                .andExpect(jsonPath("$.data[0].username").value("admin2"))
//                .andExpect(jsonPath("$.data[0].role").value("ROLE_ADMIN"))
//                .andExpect(jsonPath("$.data[0].email").value("admin@example.com"))
//                .andExpect(jsonPath("$.data[0].fullName").value("Gia Bao"))
//
//                // User 2
//                .andExpect(jsonPath("$.data[1].username").value("jane"))
//                .andExpect(jsonPath("$.data[1].role").value("ROLE_USER"))
//                .andExpect(jsonPath("$.data[1].email").value("jane@example.com"))
//                .andExpect(jsonPath("$.data[1].fullName").value("Jane Smith"));
//
//        verify(userManagementService).getAllUsers();
//        verifyNoMoreInteractions(userManagementService);
//    }
//
//    @Test
//    @WithMockUser(username = "user", roles = {"USER"})
//    void getAllUsers_shouldReturn403_whenUser() throws Exception {
//        mockMvc.perform(get(API))
//                .andExpect(status().isForbidden());
//    }
//
////    Không có JWT / không có user. Gán AnonymousAuthenticationToken
////    Kiểm tra hasRole('ADMIN'). Anonymous không có role ADMIN
////    ⇒ Access Denied → 403
////  ➡️ Không phải unauthenticated, mà là authenticated as ANONYMOUS
//    @Test
//    void getAllUsers_shouldReturn401_whenUnauthenticated() throws Exception {
//        mockMvc.perform(get(API))
//                .andExpect(status().isForbidden());
//    }
//
//    // =========================
//    // GET MY PROFILE
//    // =========================
//    @Test
//    @WithMockUser(username = "bao", roles = {"USER"})
//    void getMyProfile_shouldReturnProfile() throws Exception {
//        UserResponse res = UserResponse.builder()
//                .username("bao")
//                .role("ROLE_USER")
//                .build();
//
//        when(userProfileService.getUser("bao"))
//                .thenReturn(res);
//
//        mockMvc.perform(get(API + "/me"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.status").value(200))
//                .andExpect(jsonPath("$.message")
//                        .value("Profile retrieved successfully"))
//                .andExpect(jsonPath("$.data.username").value("bao"))
//                .andExpect(jsonPath("$.data.role").value("ROLE_USER"));
//
//        verify(userProfileService).getUser("bao");
//        verifyNoMoreInteractions(userProfileService);
//    }
//
//    @Test
//    void getMyProfile_shouldReturn403_whenUnauthenticated() throws Exception {
//        mockMvc.perform(get(API + "/me"))
//                .andExpect(status().isForbidden());
//    }
//
//    // =========================
//    // UPDATE MY PROFILE
//    // =========================
//    @Test
//    @WithMockUser(username = "bao", roles = {"USER"})
//    void updateMyProfile_shouldReturnUpdatedProfile() throws Exception {
//
//        UpdateUserRequest req = UpdateUserRequest.builder()
//                .email("new@email.com")
//                .fullName("Bao Nguyen")
//                .build();
//
//        UserResponse res = UserResponse.builder()
//                .username("bao")
//                .role("ROLE_USER")
//                .email("new@email.com")
//                .fullName("Bao Nguyen")
//                .build();
//
//        when(userProfileService.updateUser(any(UpdateUserRequest.class)))
//                .thenReturn(res);
//
//        mockMvc.perform(put(API + "/me")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(req)))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.status").value(200))
//                .andExpect(jsonPath("$.message")
//                        .value("Profile updated successfully"))
//                .andExpect(jsonPath("$.data.username").value("bao"))
//                .andExpect(jsonPath("$.data.role").value("ROLE_USER"))
//                .andExpect(jsonPath("$.data.email").value("new@email.com"))
//                .andExpect(jsonPath("$.data.fullName").value("Bao Nguyen"));
//
//        verify(userProfileService).updateUser(any(UpdateUserRequest.class));
//    }
//
//    @Test
//    void updateMyProfile_shouldReturn403_whenUnauthenticated() throws Exception {
//        mockMvc.perform(put(API + "/me"))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    @WithMockUser(username = "bao", roles = {"USER"})
//    void updateMyProfile_shouldReturn400_whenInvalidRequest() throws Exception {
//
//        UpdateUserRequest req = UpdateUserRequest.builder()
//                .email("invalid-email") // giả sử có @Email
//                .build();
//
//        mockMvc.perform(put(API + "/me")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content(objectMapper.writeValueAsString(req)))
//                .andExpect(status().isBadRequest());
//    }
//
//    // =========================
//    // DELETE USER
//    // =========================
//    @Test
//    @WithMockUser(username = "admin", roles = {"ADMIN"})
//    void deleteUser_shouldReturn204_whenAdmin() throws Exception {
//
////        doNothing().when(userManagementService).deleteUser("testuser");
//
//        mockMvc.perform(delete(API + "/testuser"))
//                .andExpect(status().isNoContent());
////                .andDo(print())
//
//        verify(userManagementService).deleteUser("testuser");
//        verifyNoMoreInteractions(userManagementService);
//    }
//
//    @Test
//    @WithMockUser(username = "user", roles = {"USER"})
//    void deleteUser_shouldReturn403_whenUser() throws Exception {
//        mockMvc.perform(delete(API + "/testuser"))
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    @WithMockUser(username = "admin", roles = {"ADMIN"})
//    void deleteUser_shouldReturn404_whenUserNotFound() throws Exception {
//
//        doThrow(new RuntimeException("User not found"))
//                .when(userManagementService)
//                .deleteUser("unknown");
//
//        mockMvc.perform(delete(API + "/unknown"))
//                .andExpect(status().isInternalServerError());
//    }
//
//}
