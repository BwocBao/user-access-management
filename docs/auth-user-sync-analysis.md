# Phân tích kỹ thuật: Auth-Service và User-Service với DB riêng

**Mục đích:** Phân tích chi tiết cơ chế đồng bộ giữa auth-service và user-service khi sử dụng database riêng, và gợi ý cải thiện.

---

## 1. Kiến trúc hiện tại

### 1.1 Cấu hình database

| Service      | Database     | Kết nối (docker-compose)                    |
|-------------|--------------|---------------------------------------------|
| auth-service | `auth_service` | `jdbc:postgresql://postgres:5432/auth_service` |
| user-service | `user_service` | `jdbc:postgresql://postgres:5432/user_service` |

- Cùng instance PostgreSQL, **hai database tách biệt**
- Entity `User` và `UserRepository` dùng chung từ `core`, nhưng mỗi service kết nối tới DB riêng → **hai bảng `users` độc lập**

### 1.2 Luồng dữ liệu hiện tại

```
┌─────────────────┐                    ┌─────────────────┐
│   auth_service   │                    │   user_service   │
│   (PostgreSQL)   │                    │   (PostgreSQL)   │
│                  │                    │                  │
│  users (auth)    │                    │  users (profile) │
└────────▲─────────┘                    └────────▲─────────┘
         │                                       │
         │ register()                            │ getUserByUsername()
         │ login()                               │ updateUser()
         │ registerRole()                        │ deleteUserByUsername()
         │                                       │
┌────────┴─────────┐                    ┌────────┴─────────┐
│  auth-service     │                    │  user-service     │
│  (port 8081)     │                    │  (port 8082)      │
└──────────────────┘                    └──────────────────┘
         │                                       │
         │  JWT (username, role)                  │  JWT validation
         └───────────────────────────────────────┘
                    Không có gọi HTTP giữa 2 service
```

---

## 2. Phân tích source code chi tiết

### 2.1 Auth-service: Tạo user

**File:** `auth-service/src/main/java/com/r2s/auth/service/AuthService.java`

```java
// register() - dòng 25-35
public void register(RegisterRequest registerRequest) {
    if(userRepository.findByUsername(registerRequest.getUsername()).isPresent()){
        throw new CustomException("Username exist");
    }
    User user = new User();
    user.setUsername(registerRequest.getUsername());
    user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
    user.setRole(Role.ROLE_USER);
    userRepository.save(user);  // → Lưu vào auth_service DB
}

// registerRole() - dòng 50-66
public void registerRole(RegisterRoleRequest req) {
    // ... tương tự, lưu vào auth_service DB
}
```

**Kết luận:** Auth-service **chỉ** ghi vào `auth_service`. Không có HTTP call, event hay message nào gửi sang user-service.

---

### 2.2 User-service: Lazy creation

**File:** `user-service/src/main/java/com/r2s/user/service/UserService.java`

```java
// getUserByUsername() - dòng 30-44
public UserResponse getUserByUsername(String username, String roleStr) {
    Role role = Role.valueOf(roleStr);  // role từ JWT

    User user = userRepository.findByUsername(username)  // Tìm trong user_service DB
            .orElseGet(() -> {
                // KHÔNG TÌM THẤY → TỰ TẠO USER MỚI
                User newUser = new User();
                newUser.setUsername(username);
                newUser.setRole(role);
                newUser.setFullName("");
                newUser.setEmail("");
                newUser.setPassword("");
                return userRepository.save(newUser);  // Lưu vào user_service DB
            });
    return userMapper.toUserResponse(user);
}

// updateUser() - dòng 46-63
public UserResponse updateUser(UpdateUserRequest req, String username, String roleStr) {
    User user = userRepository.findByUsername(username)
            .orElseGet(() -> {
                User newUser = new User();
                newUser.setUsername(username);
                newUser.setRole(role);
                newUser.setPassword("");
                return newUser;  // Chưa save, sẽ save ở dòng 61
            });
    user.setFullName(req.getFullName());
    user.setEmail(req.getEmail());
    userRepository.save(user);
    return userMapper.toUserResponse(user);
}
```

**Luồng thực tế:**
1. User đăng ký qua auth → có trong `auth_service`
2. User login → nhận JWT (username, role)
3. User gọi `GET /api/users/me` với JWT → user-service tìm trong `user_service`
4. Nếu không có → **lazy create** với username + role từ JWT

---

### 2.3 User-service: Delete không đồng bộ

**File:** `user-service/src/main/java/com/r2s/user/service/UserService.java`

```java
@Transactional
public void deleteUserByUsername(String username) {
    if (!userRepository.existsByUsername(username)) {
        throw new ResourceNotFoundExecption("User not found: " + username);
    }
    userRepository.deleteByUsername(username);  // CHỈ XÓA TRONG user_service
}
```

**Vấn đề:** User vẫn còn trong `auth_service` → có thể login lại và nhận JWT mới.

---

## 3. Các vấn đề kỹ thuật

### 3.1 Bảng tổng hợp

| # | Vấn đề | Mức độ | Mô tả |
|---|--------|--------|-------|
| 1 | **Delete không đồng bộ** | Critical | Admin xóa user trong user-service, user vẫn tồn tại trong auth → có thể login lại |
| 2 | **Lazy creation dựa trên JWT** | High | Username/role lấy từ JWT, không verify với auth DB → token cũ/giả mạo có thể tạo "ghost" user |
| 3 | **Không có sync khi register** | High | User mới đăng ký chưa có trong user-service cho đến khi gọi /me → profile rỗng, có thể gây lỗi nếu logic khác giả định user đã có |
| 4 | **Race condition khi lazy create** | Medium | Hai request đồng thời cho user mới → cả hai `findByUsername` empty → hai lần tạo → có thể vi phạm unique constraint |
| 5 | **updateUser tạo user mới** | Medium | `updateUser` cũng dùng orElseGet → user chưa từng gọi /me có thể được tạo qua PUT /me (ít gặp nhưng không nhất quán) |
| 6 | **registerRole không sync** | Medium | Tạo ADMIN trong auth, user-service chỉ biết khi user login và gọi /me |

---

### 3.2 Chi tiết từng vấn đề

#### 3.2.1 Delete không đồng bộ

- **Hiện trạng:** `deleteUserByUsername` chỉ xóa trong `user_service`
- **Hậu quả:** User vẫn login được, JWT mới, và lazy create lại profile trong user-service
- **Nguyên nhân:** Không có giao tiếp auth-service ↔ user-service

#### 3.2.2 Lazy creation dựa trên JWT

- **Rủi ro:** JWT có thể bị lấy cắp, chưa hết hạn sau khi user bị xóa, hoặc role bị sửa
- **Hậu quả:** User-service tạo user với thông tin từ JWT mà không kiểm tra auth DB

#### 3.2.3 Race condition

```java
// Thread 1 và Thread 2 cùng gọi getUserByUsername("newuser")
// Cả hai: findByUsername → Optional.empty()
// Cả hai: orElseGet → tạo User mới
// Thread 1: save(newUser1) → OK
// Thread 2: save(newUser2) → Có thể DataIntegrityViolationException (unique username)
```

---

## 4. Gợi ý cải thiện chi tiết

### 4.1 Phương án A: Sync qua HTTP (đồng bộ khi register)

**Ý tưởng:** Khi auth-service tạo user thành công, gọi user-service để tạo profile tương ứng.

**Cấu hình:**

```yaml
# application.yaml (auth-service)
app:
  user-service:
    url: ${USER_SERVICE_URL:http://user-service:8082}
```

**AuthService.java – thêm sync sau khi register:**

```java
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;  // Hoặc WebClient
    
    @Value("${app.user-service.url}")
    private String userServiceUrl;

    public void register(RegisterRequest registerRequest) {
        if (userRepository.findByUsername(registerRequest.getUsername()).isPresent()) {
            throw new CustomException("Username exist");
        }
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole(Role.ROLE_USER);
        userRepository.save(user);
        
        // Sync sang user-service
        syncUserToUserService(user.getUsername(), user.getRole().name());
    }

    private void syncUserToUserService(String username, String role) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            // Internal API key hoặc service-to-service auth
            headers.set("X-Internal-Service-Key", internalApiKey);
            
            Map<String, String> body = Map.of("username", username, "role", role);
            ResponseEntity<Void> response = restTemplate.exchange(
                userServiceUrl + "/internal/users/sync",
                HttpMethod.POST,
                new HttpEntity<>(body, headers),
                Void.class
            );
            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Failed to sync user {} to user-service: {}", username, response.getStatusCode());
                // Cân nhắc: rollback user trong auth hoặc retry/queue
            }
        } catch (Exception e) {
            log.error("Error syncing user {} to user-service", username, e);
            // Retry, dead-letter queue, hoặc eventual consistency
        }
    }
}
```

**User-service – endpoint internal:**

```java
@RestController
@RequestMapping("/internal/users")
public class InternalUserController {
    
    @PostMapping("/sync")
    public ResponseEntity<Void> syncUser(@RequestBody SyncUserRequest req) {
        if (userRepository.findByUsername(req.getUsername()).isEmpty()) {
            User user = new User();
            user.setUsername(req.getUsername());
            user.setRole(Role.valueOf(req.getRole()));
            user.setFullName("");
            user.setEmail("");
            user.setPassword("");
            userRepository.save(user);
        }
        return ResponseEntity.ok().build();
    }
}
```

**Bảo mật:** Bảo vệ `/internal/**` bằng API key, mạng nội bộ, hoặc mTLS.

---

### 4.2 Phương án B: Verify với auth-service trước khi lazy create

**Ý tưởng:** User-service không tin JWT mù quáng; trước khi lazy create, gọi auth-service để xác nhận user tồn tại.

**UserService.java – sửa getUserByUsername:**

```java
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final AuthServiceClient authServiceClient;  // Feign/WebClient

    public UserResponse getUserByUsername(String username, String roleStr) {
        User user = userRepository.findByUsername(username)
                .orElseGet(() -> {
                    // Verify user tồn tại trong auth trước khi tạo
                    if (!authServiceClient.existsByUsername(username)) {
                        throw new ResourceNotFoundException("User not found: " + username);
                    }
                    User newUser = new User();
                    newUser.setUsername(username);
                    newUser.setRole(Role.valueOf(roleStr));
                    newUser.setFullName("");
                    newUser.setEmail("");
                    newUser.setPassword("");
                    return userRepository.save(newUser);
                });
        return userMapper.toUserResponse(user);
    }
}
```

**Auth-service – endpoint verify:**

```java
@GetMapping("/internal/users/{username}/exists")
public ResponseEntity<Boolean> existsByUsername(@PathVariable String username) {
    boolean exists = userRepository.findByUsername(username).isPresent();
    return ResponseEntity.ok(exists);
}
```

---

### 4.3 Phương án C: Delete đồng bộ (gọi auth-service khi xóa)

**UserService.java – sửa deleteUserByUsername:**

```java
@Transactional
public void deleteUserByUsername(String username) {
    if (!userRepository.existsByUsername(username)) {
        throw new ResourceNotFoundException("User not found: " + username);
    }
    // 1. Xóa trong auth trước (source of truth)
    authServiceClient.deleteUser(username);
    // 2. Xóa trong user-service
    userRepository.deleteByUsername(username);
}
```

**Lưu ý:** Cần xử lý trường hợp auth xóa thành công nhưng user-service xóa thất bại (retry, compensation).

---

### 4.4 Phương án D: Xử lý race condition khi lazy create

```java
public UserResponse getUserByUsername(String username, String roleStr) {
    return userRepository.findByUsername(username)
            .map(userMapper::toUserResponse)
            .orElseGet(() -> createUserIfNotExists(username, roleStr));
}

private UserResponse createUserIfNotExists(String username, String roleStr) {
    try {
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setRole(Role.valueOf(roleStr));
        newUser.setFullName("");
        newUser.setEmail("");
        newUser.setPassword("");
        User saved = userRepository.saveAndFlush(newUser);
        return userMapper.toUserResponse(saved);
    } catch (DataIntegrityViolationException e) {
        // Race: user đã được tạo bởi request khác
        return userRepository.findByUsername(username)
                .map(userMapper::toUserResponse)
                .orElseThrow(() -> new RuntimeException("Failed to create or find user", e));
    }
}
```

---

### 4.5 Phương án E: Event-driven (Kafka/RabbitMQ)

**Ý tưởng:** Auth-service publish event `UserRegistered`; user-service subscribe và tạo profile.

```
auth-service --[UserRegistered]--> Kafka --> user-service (consumer)
```

- **Ưu:** Tách biệt, có thể retry, mở rộng
- **Nhược:** Thêm Kafka, xử lý eventual consistency

---

## 5. Khuyến nghị triển khai

| Ưu tiên | Hạng mục | Phương án | Ước lượng (giờ) |
|---------|----------|-----------|------------------|
| 1 | Delete đồng bộ | Phương án C – user-service gọi auth-service khi xóa | 4–6 |
| 2 | Sync khi register | Phương án A – auth gọi user-service sau khi register | 4–6 |
| 3 | Verify trước lazy create | Phương án B – user-service verify với auth trước khi tạo | 3–4 |
| 4 | Race condition | Phương án D – bắt `DataIntegrityViolationException` và retry find | 1–2 |
| 5 | (Tùy chọn) Event-driven | Phương án E – dùng message queue | 8–12 |

**Thứ tự gợi ý:**
1. **Phương án C** – sửa delete để đồng bộ (ảnh hưởng bảo mật trực tiếp)
2. **Phương án A** – sync khi register (giảm phụ thuộc lazy create)
3. **Phương án D** – xử lý race khi lazy create
4. **Phương án B** – verify với auth trước khi tạo (tăng độ tin cậy)

---

## 6. Checklist triển khai

- [ ] Thêm `RestTemplate`/`WebClient` với timeout
- [ ] Cấu hình `USER_SERVICE_URL`, `AUTH_SERVICE_URL` qua biến môi trường
- [ ] Bảo vệ endpoint `/internal/**` (API key, network policy)
- [ ] Implement sync khi register (Phương án A)
- [ ] Implement delete đồng bộ (Phương án C)
- [ ] Xử lý race condition (Phương án D)
- [ ] (Tùy chọn) Verify với auth trước lazy create (Phương án B)
- [ ] Unit test cho sync logic
- [ ] Integration test cho flow register → sync → get profile
- [ ] Integration test cho flow delete → verify không login được
