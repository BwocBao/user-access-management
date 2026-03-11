//package com.r2s.auth.unit.controller;
//
//import com.r2s.auth.controller.RoleController;
//import com.r2s.core.security.JwtFilter;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.context.annotation.Import;
//import org.springframework.security.test.context.support.WithMockUser;
//import org.springframework.test.web.servlet.MockMvc;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@WebMvcTest(controllers = RoleController.class)
//@AutoConfigureMockMvc(addFilters = true)
//class RoleControllerTest {
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockBean
//    private JwtFilter jwtFilter;
//
//    // =========================
//    // USER ACCESS
//    // =========================
//
//    @Test
//    @WithMockUser(roles = "USER")
//    void userAccess_shouldReturn200_whenRoleUser() throws Exception {
//
//        mockMvc.perform(get("/role/user"))
//                .andExpect(status().isOk())
//                .andExpect(content().string("Hello USER"));
//    }
//
//    @Test
//    @WithMockUser(roles = "ADMIN")
//    void userAccess_shouldReturn403_whenNotUser() throws Exception {
//
//        mockMvc.perform(get("/role/user"))
//                .andExpect(status().isForbidden());
//    }
//
//    // =========================
//    // ADMIN ACCESS
//    // =========================
//
//    @Test
//    @WithMockUser(roles = "ADMIN")
//    void adminAccess_shouldReturn200_whenAdmin() throws Exception {
//
//        mockMvc.perform(get("/role/admin"))
//                .andExpect(status().isOk())
//                .andExpect(content().string("Hello ADMIN"));
//    }
//
//    @Test
//    @WithMockUser(roles = "USER")
//    void adminAccess_shouldReturn403_whenNotAdmin() throws Exception {
//
//        mockMvc.perform(get("/role/admin"))
//                .andExpect(status().isForbidden());
//    }
//
//    // =========================
//    // MODERATOR ACCESS
//    // =========================
//
//    @Test
//    @WithMockUser(roles = "MODERATOR")
//    void modAccess_shouldReturn200_whenModerator() throws Exception {
//
//        mockMvc.perform(get("/role/mod"))
//                .andExpect(status().isOk())
//                .andExpect(content().string("Hello MODERATOR"));
//    }
//
//    @Test
//    @WithMockUser(roles = "USER")
//    void modAccess_shouldReturn403_whenNotModerator() throws Exception {
//
//        mockMvc.perform(get("/role/mod"))
//                .andExpect(status().isForbidden());
//    }
//}