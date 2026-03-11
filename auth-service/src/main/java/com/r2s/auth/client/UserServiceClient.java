package com.r2s.auth.client;

import com.r2s.core.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final WebClient webClient;
    private final JwtUtil jwtUtil;

    @Value("${app.user-service.url}")
    private String userServiceUrl;

    public void syncUser(String username) {
        String token = jwtUtil.generateServiceToken("auth-service");

        webClient.post()
                .uri(userServiceUrl + "/internal/auth/sync/{username}", username)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        response -> Mono.error(new RuntimeException("Sync failed")))
                .bodyToMono(Void.class)
                .block();
    }
}
