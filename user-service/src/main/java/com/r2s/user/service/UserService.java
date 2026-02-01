package com.r2s.user.service;

import com.r2s.core.entity.Role;
import com.r2s.core.entity.User;
import com.r2s.core.exception.ResourceNotFoundExecption;
import com.r2s.core.repository.UserRepository;
import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.dto.UserResponse;
import com.r2s.user.mapper.UserMapper;
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
    private final UserMapper userMapper;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

    public  UserResponse getUserByUsername(String username,String roleStr) {
        Role role = Role.valueOf(roleStr); //ENUM

        User user = userRepository.findByUsername(username)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUsername(username);
                    newUser.setRole(role);      //ENUM
                    newUser.setFullName("");
                    newUser.setEmail("");
                    newUser.setPassword("");
                    return userRepository.save(newUser);
                });
        return userMapper.toUserResponse(user);
    }

    public UserResponse updateUser(UpdateUserRequest req, String username,String roleStr) {
        Role role = Role.valueOf(roleStr); //ENUM

        User user = userRepository.findByUsername(username)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUsername(username);
                    newUser.setRole(role); // ENUM
                    newUser.setPassword("");
                    return newUser;
                });

        user.setFullName(req.getFullName());
        user.setEmail(req.getEmail());

        userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    @Transactional
    public void deleteUserByUsername(String username) {
        if (!userRepository.existsByUsername(username)) {
            throw new ResourceNotFoundExecption("User not found: " + username);
        }
        userRepository.deleteByUsername(username);
    }
}
