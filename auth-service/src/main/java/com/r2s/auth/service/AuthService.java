package com.r2s.auth.service;

import com.r2s.auth.dto.AuthResponse;
import com.r2s.auth.dto.LoginRequest;
import com.r2s.auth.dto.RegisterRequest;

import com.r2s.auth.entity.User;
import com.r2s.auth.repository.UserRepository;
import com.r2s.auth.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public void register(RegisterRequest registerRequest) {
        if(userRepository.findByUsername(registerRequest.getUsername()).isPresent()){
            throw new RuntimeException("Username exist");
        }

        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRole("ROLE_USER");
        userRepository.save(user);
    }

    public AuthResponse login(LoginRequest loginRequest) {
        User user=userRepository.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Not found"));

        if(!passwordEncoder.matches(loginRequest.getPassword(),user.getPassword())){
            throw new RuntimeException("Wrong password");
        }

        String accessToken = jwtUtil.generateToken(user.getUsername());
        return new AuthResponse(accessToken);

    }

//    public void registerRole(RegisterRoleRequest req) {
//        if(userRepository.findByUsername(req.getUsername()).isPresent()){
//            throw new RuntimeException("Username exist");
//        }
//
//        User user = new User();
//        Role role;
//        try {
//            role = Role.valueOf(req.getRole().toUpperCase());
//        } catch (IllegalArgumentException e) {
//            throw new RuntimeException("Invalid role");
//        }
//        user.setUsername(req.getUsername());
//        user.setPassword(passwordEncoder.encode(req.getPassword()));
//        user.setRole(role);
//        userRepository.save(user);
//    }
}
