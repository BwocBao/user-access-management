package com.r2s.user.service;

import com.r2s.core.entity.Role;
import com.r2s.core.entity.User;
import com.r2s.core.repository.UserRepository;
import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.dto.UserRequest;
import com.r2s.user.dto.UserResponse;
import com.r2s.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserManagementService, UserProfileService{
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserResponse createUser(UserRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        log.debug("Creating new user with username: {}", request.getUsername());

        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setPassword(request.getPassword());
        newUser.setRole(request.getRole());
        newUser.setEmail(request.getEmail());
        newUser.setFullName(request.getFullName());

        return userMapper.toUserResponse(userRepository.save(newUser));
    }

    @Override
    @Transactional
    public void deleteUser(String username) {

        if (!userRepository.existsByUsername(username)) {
            throw new RuntimeException("User not found");
        }

        log.debug("Deleting user with username: {}", username);

        try{
            userRepository.deleteByUsername(username);
        }
        catch(Exception ex){
            log.error("Failed to delete user: {}", username, ex);
            throw new RuntimeException("User deletion failed");
        }

    }

    @Override
    public List<UserResponse> getAllUsers() {
        log.debug("Retrieving all users");
        return userRepository.findAll()
                .stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

    @Transactional
    @Override
    public UserResponse updateUser(UpdateUserRequest request) {

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        String username = authentication.getName();

        log.debug("Updating user with username: {}", username);

        Role role = Role.valueOf(
                authentication.getAuthorities()
                        .iterator()
                        .next()
                        .getAuthority()
        );

        User user = getOrCreateUser(username, role);

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Override
    public UserResponse getUser(String username) {
        log.debug("Retrieving user with username: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        return userMapper.toUserResponse(user);
    }

    private User getOrCreateUser(String username, Role role) {

        return userRepository.findByUsername(username)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setUsername(username);
                    newUser.setRole(role); // lấy từ JWT
                    newUser.setEmail(null);
                    newUser.setFullName("");
                    newUser.setPassword("");
                    return userRepository.save(newUser);
                });
    }
}
