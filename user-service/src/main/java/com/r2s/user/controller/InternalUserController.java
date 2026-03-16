package com.r2s.user.controller;

import com.r2s.user.entity.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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

    @PostMapping("/sync/{username}")
    @PreAuthorize("hasRole('SERVICE')")
    public ResponseEntity<Void> createUser(@PathVariable("username") String username) {

        try {
            userRepository.save(
                    UserProfile.builder()
                            .username(username)
                            .build()
            );
        } catch (DataIntegrityViolationException e) {
            // user đã tồn tại → ignore
        }

        return ResponseEntity.ok().build();
    }
}
