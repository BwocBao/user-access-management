package com.r2s.user.controller;

import com.r2s.user.entity.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;
import com.r2s.user.repository.UserProfileRepository;

@RestController
@RequestMapping("/internal/auth")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserProfileRepository userRepository;

    @PreAuthorize("hasRole('SERVICE')")
    @PostMapping("/sync/{username}")
    public ResponseEntity<Void> createUser(@PathVariable("username") String username) {

        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.ok().build();
        }

        userRepository.save(
                UserProfile.builder()
                        .username(username)
                        .build()
        );

        return ResponseEntity.ok().build();
    }
}
