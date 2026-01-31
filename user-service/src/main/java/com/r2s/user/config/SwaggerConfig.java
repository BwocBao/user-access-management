package com.r2s.user.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI(
            @Value("${open.api.title:API}") String title,
            @Value("${open.api.version:1.0}") String version,
            @Value("${open.api.description:Base API}") String description,
            @Value("${open.api.server-url:http://localhost:8080}") String serverUrl
    ) {

        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .info(
                        new Info()
                                .title(title).version(version).description(description)
                                .license(new License().name("API License").url("https://domain.vn/license")))
                .servers(List.of(new Server().url(serverUrl).description("Public Api")
                ))
                .components(
                        new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }

    /**
     * Group API cho từng service
     * Mỗi service chỉ cần override packagesToScan
     */
    @Bean
    public GroupedOpenApi baseApiGroup() {
        return GroupedOpenApi.builder()
                .group("user-api")
                .pathsToMatch("/**")
                .build();
    }
}