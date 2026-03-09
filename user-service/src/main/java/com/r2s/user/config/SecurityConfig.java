package com.r2s.user.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.r2s.core.dto.ApiResponse;
import com.r2s.core.security.JwtFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)//bật phân quyền cho method
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtFilter jwtFilter;
    private final ObjectMapper objectMapper;
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        authorizeRequests ->
                                authorizeRequests
                                        .requestMatchers("/api/auth/**","/api/users/hello").permitAll()
                                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {

                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.setContentType("application/json");

                            ApiResponse<?> response =
                                    ApiResponse.error(
                                            HttpStatus.UNAUTHORIZED.value(),
                                            "Unauthorized or token missing or invalid",
                                            null
                                    );

                            objectMapper.writeValue(res.getOutputStream(), response);
                            res.flushBuffer();

                        })
                        .accessDeniedHandler((req, res, e) -> {

                            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            res.setContentType("application/json");

                            ApiResponse<?> response =
                                    ApiResponse.error(
                                            HttpStatus.FORBIDDEN.value(),
                                            "Forbidden: not enough permissions",
                                            null
                                    );

                            objectMapper.writeValue(res.getOutputStream(), response);
                            res.flushBuffer();

                        })
                );
        return http.build();
    }

    // WebSecurityCustomizer bỏ qua toàn bộ Security Filter Chain những request dưới ko cần vào Security Filter Chain
    // Vì nếu cho các request đó vào Security Filter Chain rồi mới thấy permit thì có thể ko hiệu quả hoặc gây ra lỗi
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web ->
                web.ignoring()
                        // Swagger UI (Springdoc OpenAPI)
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/swagger-initializer/**",
                                "/webjars/**")
                        // Actuator (health check)
                        .requestMatchers("/actuator/**")
                        // Static resources
                        .requestMatchers(
                                "/favicon.ico",
                                "/error",
                                "/css/**",
                                "/js/**",
                                "/images/**",
                                "/fonts/**",
                                "/assets/**");
    }
}
