package com.r2s.auth.unit.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.dto.RegisterRequest;
import com.r2s.auth.dto.RegisterRoleRequest;
import com.r2s.core.security.JwtFilter;
import com.r2s.core.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
public class AuthControllerItegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;


    // ============================
    // PostgreSQL Container
    // ============================
    @Container//Nói với JUnit:"Thằng này là container→tự start/stop cho tao".Không cần viết:postgres.start();/postgres.stop();
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("test_db")
                    .withUsername("postgres")
                    .withPassword("123456");

    // ============================
    // Inject DB config cho Spring
    // ============================
    //URL, port… đều RANDOM mỗi lần chạy. Không thể hardcode.
    @DynamicPropertySource//Để inject runtime config.Chạy trước khi Spring Context init.Override application.yml.
    static void overrideProps(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url",
                postgres::getJdbcUrl);

        registry.add("spring.datasource.username",
                postgres::getUsername);

        registry.add("spring.datasource.password",
                postgres::getPassword);

        registry.add("spring.jpa.hibernate.ddl-auto",
                () -> "update");
    }

    // ============================
    // TEST HELLO
    // ============================
    @Test
    void ping_shouldReturnHello() throws Exception {

        mockMvc.perform(get("/api/auth/hello"))
                .andExpect(status().isOk())
                .andExpect(content()
                        .string("Hello from Auth Service"));
    }


    // ============================
    // TEST REGISTER
    // ============================
    @Test
    void register_shouldReturn200_whenValid() throws Exception {

        RegisterRequest request = new RegisterRequest();
        request.setUsername("test");
        request.setPassword("123456");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message")
                        .value("User registered successfully"))
                .andExpect(jsonPath("$.data").isEmpty());

    }

    @Test
    void regiser_shouldReturn400_whenInvalid() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("");
        request.setPassword("");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registerRole_shouldReturn200_whenValid() throws Exception {

        RegisterRoleRequest request = new RegisterRoleRequest();
        request.setUsername("Danh");
        request.setPassword("123456");
        request.setRole("Role_Admin");

        mockMvc.perform(post("/api/auth/register/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message")
                        .value("User registered successfully"))
                .andExpect(jsonPath("$.data").isEmpty());

    }

    @Test
    void registerRole_shouldReturn400_whenRoleInvalid() throws Exception {

        RegisterRoleRequest request = new RegisterRoleRequest();
        request.setUsername("huy");
        request.setPassword("123456");
        request.setRole("Role_superman");

        mockMvc.perform(post("/api/auth/register/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid role"));

    }

    // ============================
    // TEST LOGIN
    // ============================
    @Test
    void login_shouldReturn200_whenValid() throws Exception {

        // 1. Register trước
        RegisterRequest register = new RegisterRequest();
        register.setUsername("bwocbao");
        register.setPassword("123456");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register)));


        // 2. Login
        LoginRequest login = new LoginRequest();
        login.setUsername("bwocbao");
        login.setPassword("123456");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message")
                        .value("User logged successfully"))
                .andExpect(jsonPath("$.data.token")
                        .exists());
    }

    @Test
    void login_shouldReturn401_whenUsernameNotExist() throws Exception {
        // 1. Login
        LoginRequest login = new LoginRequest();
        login.setUsername("super");
        login.setPassword("123456");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))

                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Username does not exist"));
    }

    @Test
    void login_shouldReturn401_whenWrongPass() throws Exception {

        // 1. Register trước
        RegisterRequest register = new RegisterRequest();
        register.setUsername("bao");
        register.setPassword("123456");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register)));


        // 2. Login
        LoginRequest login = new LoginRequest();
        login.setUsername("bao");
        login.setPassword("123457");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))

                .andExpect(status().isUnauthorized())
                .andExpect(content().string("Wrong password"));
    }

}
//1️⃣ JUnit tạo object test như thế nào?
//JUnit KHÔNG dùng 1 object test cho tất cả @Test.
//Mặc định: 👉 Mỗi @Test = 1 instance mới.
//Ví dụ:
//class MyTest {
//
//    @Test
//    void test1(){}
//
//    @Test
//    void test2(){}
//}
//Thực tế JUnit làm:
//  new MyTest() → chạy test1
//  new MyTest() → chạy test2
//Nó KHÔNG reuse object.
//Lý do: tránh test ảnh hưởng lẫn nhau.
//2️⃣ Nếu container KHÔNG static thì chuyện gì xảy ra?
//Giả sử bạn viết:
//@PostgreSQLContainer
//PostgreSQLContainer<?> postgres =
//        new PostgreSQLContainer<>("postgres"); (không static)
//
//Khi test chạy:
//test1: new TestClass() → postgres được tạo → start docker → chạy test1 → stop
//test2: new TestClass() → postgres tạo lại → start docker lại → chạy test2 → stop
//Kết quả:
//Mỗi test = 1 container
//❌ Mỗi container start = 5–15 giây
//❌ 10 test = 150s 😵
//Rất chậm.
//3️⃣ Khi dùng static thì sao?
//static PostgreSQLContainer<?> postgres = ...
//static = thuộc về class, không thuộc object.
//JVM load class:
//TestClass được load → static field init → postgres tạo 1 lần
//Khi chạy test:
//  new TestClass() → test1 (dùng chung postgres)
//  new TestClass() → test2 (dùng chung postgres)
//Thực tế: 1 container chạy N test
//✅ Nhanh
//✅ Tiết kiệm RAM
//✅ Chuẩn best practice