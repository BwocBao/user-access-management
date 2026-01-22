package com.r2s.user.service;

import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.dto.UserResponse;
import com.r2s.user.entity.Role;
import com.r2s.user.entity.User;
import com.r2s.user.mapper.UserMapper;
import com.r2s.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream().map(UserMapper::toUserResponse).toList();
    }

    public  UserResponse getUserByUsername(String username) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        String roleStr = auth.getAuthorities()
                .iterator()
                .next()
                .getAuthority(); // ROLE_ADMIN

        Role role = Role.valueOf(roleStr); // ✅ ENUM

        User user = userRepository.findByUsername(username)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUsername(username);
                    newUser.setRole(role);      // ⭐ ENUM
                    newUser.setFullName("");
                    newUser.setEmail("");
                    return userRepository.save(newUser);
                });
        return UserMapper.toUserResponse(user);
    }

    public UserResponse updateUser(UpdateUserRequest req, String username) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Role role = Role.valueOf(
                auth.getAuthorities().iterator().next().getAuthority()
        );

        User user = userRepository.findByUsername(username)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUsername(username);
                    newUser.setRole(role); // ENUM
                    return newUser;
                });

        user.setFullName(req.getFullName());
        user.setEmail(req.getEmail());

        userRepository.save(user);
        return UserMapper.toUserResponse(user);
    }

    @Transactional
    public void deleteUserByUsername(String username) {
        userRepository.deleteByUsername(username);
    }
}
