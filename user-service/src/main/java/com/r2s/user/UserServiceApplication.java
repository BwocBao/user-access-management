package com.r2s.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication(scanBasePackages = {"com.r2s.user", "com.r2s.core"})
//@EnableJpaRepositories(basePackages = "com.r2s.core.repository")
//@EntityScan(basePackages = "com.r2s.core.entity")
public class UserServiceApplication
{
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
