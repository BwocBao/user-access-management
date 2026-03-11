package com.r2s.user.client;

import com.r2s.core.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class AuthServiceClient {

    private final WebClient webClient;
    private final JwtUtil jwtUtil;

    @Value("${app.auth-service.url}")
    private String authServiceUrl;

    public void deleteUser(String username) {

        webClient.delete()
                .uri(authServiceUrl + "/internal/users/{username}", username)
                .header("Authorization", "Bearer " + jwtUtil.generateServiceToken("user-service"))
                .retrieve()
                .onStatus(HttpStatusCode::isError,
                        ClientResponse::createException)
                .toBodilessEntity()
                .block();
    }

    public boolean existsByUsername(String username) {

        Boolean result = webClient.get()
                .uri(authServiceUrl + "/internal/users/{username}/exists", username)
                .retrieve()
                .bodyToMono(Boolean.class)
                .block();

        return Boolean.TRUE.equals(result);
    }
}
