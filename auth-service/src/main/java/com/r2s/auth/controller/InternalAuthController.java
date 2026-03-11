package com.r2s.auth.controller;

import com.r2s.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalAuthController {

    private final UserRepository userRepository;

    @DeleteMapping("/{username}")
    public ResponseEntity<Void> deleteUser(@PathVariable String username) {

        userRepository.findByUsername(username)
                .ifPresent(userRepository::delete);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/{username}/exists")
    public ResponseEntity<Boolean> exists(@PathVariable String username) {

        boolean exists = userRepository.findByUsername(username).isPresent();

        return ResponseEntity.ok(exists);
    }
}
