package com.r2s.auth.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@Profile("!unittest") // chỉ bật khi KHÔNG test/ Bên test khi chạy sẽ tự disable mấy cái có profile=unittest
@EnableJpaRepositories(basePackages = {
        "com.r2s.auth.repository",
        "com.r2s.core.repository"
})
@EntityScan(basePackages = {
        "com.r2s.auth.entity",
        "com.r2s.core.entity"
})
public class JpaConfig {
}
