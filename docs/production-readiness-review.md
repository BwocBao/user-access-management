# Đánh giá mã nguồn trước khi triển khai Production

**Dự án:** User Access Management (giabao)  
**Ngày đánh giá:** 4/3/2025  
**Phạm vi:** Coding và Testing  
**Mục đích:** Rà soát toàn bộ mã nguồn trước khi triển khai lên môi trường production

---

## Tổng quan dự án

- **Công nghệ:** Java 17, Spring Boot 3.3.5, PostgreSQL, JWT (jjwt 0.11.5)
- **Cấu trúc:** Multi-module Maven (core, auth-service, user-service)
- **Kiến trúc:** Layered (Controller → Service → Repository), shared core

---

## Bảng đánh giá chi tiết

| Module/Package/File | Nội dung cần cải thiện | Mức độ ưu tiên | Thời gian (giờ) | Gợi ý cải thiện | Checklist hoàn thành |
|---------------------|------------------------|----------------|-----------------|------------------|----------------------|
| **core/exception/ResourceNotFoundExecption.java** | Lỗi chính tả tên class: `Execption` → `Exception` | Cao | 0.5 | Đổi tên class thành `ResourceNotFoundException`, cập nhật tất cả reference (GlobalExceptionHandler, UserService, UserServiceUnitTest) | ☐ |
| **core/dto/ApiResponse.java** | `ApiResponse.created()` dùng status 204 (No Content) thay vì 201 (Created) | Cao | 0.25 | Đổi `status(204)` thành `status(201)` trong method `created()`; 201 là chuẩn HTTP cho tạo resource thành công | ☐ |
| **core/exception/GlobalExceptionHandler.java** | `handleException(Exception)` trả `e.getMessage()` ra client → rò rỉ thông tin nội bộ | Cao | 1 | Trả message generic (vd: "Internal server error"); log chi tiết `e.getMessage()` và stack trace ra server log bằng SLF4J | ☐ |
| **core/exception/GlobalExceptionHandler.java** | Định dạng response lỗi không thống nhất với `ApiResponse<T>` | Trung bình | 2 | Tạo `ApiResponse.error(status, message, errors)`; các handler trả `ResponseEntity<ApiResponse<?>>` thay vì `String`/`Map` để client nhận format nhất quán | ☐ |
| **core/security/JwtFilter.java** | Dùng `System.out.println` thay vì logger | Cao | 0.5 | Thêm `private static final Logger log = LoggerFactory.getLogger(JwtFilter.class)`; thay `System.out.println` bằng `log.warn("JWT validation failed", ex)` | ☐ |
| **core/security/JwtFilter.java** | Khi JWT invalid, chỉ set attribute, không trả 401 ngay | Trung bình | 1 | Cân nhắc: khi catch exception, có thể return 401 response trước khi `doFilter`; hoặc đảm bảo `AuthenticationEntryPoint` xử lý `SECURITY_EXCEPTION` | ☐ |
| **auth-service, user-service/application-docker.yaml** | `show-sql: true` trong profile docker (dùng cho deploy) | Cao | 0.25 | Đặt `show-sql: false` hoặc dùng `logging.level.org.hibernate.SQL: DEBUG` chỉ khi cần debug; tránh log SQL ra console trong production | ☐ |
| **auth-service, user-service/application-docker.yaml** | `ddl-auto: update` có thể gây rủi ro schema trong production | Cao | 2 | Xem xét dùng `validate` hoặc `none`; triển khai Flyway/Liquibase cho migration có kiểm soát | ☐ |
| **auth-service, user-service/config/SecurityConfig.java** | Whitelist `/actuator/**` nhưng không có dependency actuator | Trung bình | 0.5 | Thêm `spring-boot-starter-actuator` vào pom.xml nếu cần health/metrics; hoặc bỏ rule `/actuator/**` nếu không dùng | ☐ |
| **user-access-management/pom.xml** | Thiếu cấu hình JaCoCo cho test coverage | Trung bình | 1 | Thêm maven plugin JaCoCo; đặt mục tiêu coverage tối thiểu (vd: 70%); chạy `mvn verify` trong CI | ☐ |
| **core** | Thiếu unit test cho GlobalExceptionHandler | Trung bình | 2 | Tạo `GlobalExceptionHandlerTest`: mock các exception, gọi handler, assert status và body | ☐ |
| **core/security** | Thiếu unit test cho JwtFilter, JwtUtil | Trung bình | 3 | Test JwtFilter: token hợp lệ/không hợp lệ/hết hạn; test JwtUtil: extract username, roles, validate | ☐ |
| **core** | Thiếu unit test cho UserMapper | Thấp | 1 | Tạo `UserMapperTest`: map User → UserResponse với các field khác nhau | ☐ |
| **auth-service** | Thiếu unit test cho CustomUserDetailsService (nếu có sử dụng) | Thấp | 1 | Kiểm tra xem component có được dùng không; nếu có thì thêm test | ☐ |
| **auth-service/controller/AuthControllerUnitTest** | `addFilters = false` → không test security filter | Trung bình | 1 | Cân nhắc: khi cần test security, dùng `addFilters = true` và mock JwtFilter/JwtUtil; hoặc tách test security riêng | ☐ |
| **auth-service/integration/AuthControllerIntegrationTest.java** | Lỗi chính tả tên test: `regiser_shouldReturn400` | Thấp | 0.25 | Đổi thành `register_shouldReturn400_whenInvalid` | ☐ |
| **user-service/integration/UserControllerIntegrationTest.java** | `ROLE_USer` (typo) thay vì `ROLE_USER` | Trung bình | 0.25 | Sửa thành `ROLE_USER` để trùng với enum Role | ☐ |
| **user-service/service/UserService.java** | `getUserByUsername` và `updateUser` tự tạo user khi không tìm thấy → có thể gây divergence với auth-service | Trung bình | 2 | Xem xét: nếu auth và user dùng DB riêng, cần cơ chế sync rõ ràng; tránh lazy creation tùy tiện có thể tạo user không tồn tại trong auth | ☐ |
| **user-service/pom.xml** | Dependency test `auth-service` với scope test | Thấp | 0.5 | Đánh giá mục đích: nếu chỉ để generate token cho integration test, có thể tách logic tạo token ra helper hoặc dùng JwtUtil trực tiếp | ☐ |
| **Toàn hệ thống** | Thiếu structured logging | Trung bình | 2 | Cấu hình Logback với JSON layout; log request ID, user, action; thêm MDC cho trace | ☐ |
| **Toàn hệ thống** | Thiếu Spring Boot Actuator cho health/metrics | Trung bình | 1 | Thêm `spring-boot-starter-actuator`; cấu hình `management.endpoints.web.exposure.include: health,info`; expose health cho load balancer | ☐ |
| **auth-service, user-service** | Thiếu cấu hình application-prod.yaml | Trung bình | 1 | Tạo `application-prod.yaml`: tắt show-sql, ddl-auto: validate, cấu hình logging, actuator | ☐ |


---

## Tóm tắt theo mức độ ưu tiên

### Cao (ưu tiên triển khai trước)
- Sửa typo `ResourceNotFoundExecption` → `ResourceNotFoundException`
- Sửa `ApiResponse.created()` status 204 → 201
- Không trả `e.getMessage()` ra client trong GlobalExceptionHandler
- Thay `System.out.println` bằng logger trong JwtFilter
- Tắt `show-sql` trong application-docker.yaml
- Xem xét `ddl-auto` cho production

### Trung bình
- Chuẩn hóa format response lỗi với ApiResponse
- Thêm unit test cho GlobalExceptionHandler, JwtFilter, JwtUtil, UserMapper
- Cấu hình JaCoCo cho test coverage
- Thêm Spring Boot Actuator
- Structured logging
- Tạo application-prod.yaml
- Sửa typo `ROLE_USer` trong test

### Thấp
- Sửa typo `regiser_` trong AuthControllerIntegrationTest
- Unit test cho UserMapper, CustomUserDetailsService
- Đánh giá dependency auth-service trong user-service

---

## Ước lượng tổng thời gian (Fresher)

| Nhóm | Số hạng mục | Tổng giờ ước lượng |
|------|--------------|---------------------|
| Coding | 12 | ~12 |
| Testing | 8 | ~12 |
| **Tổng** | **20** | **~24 giờ** |

