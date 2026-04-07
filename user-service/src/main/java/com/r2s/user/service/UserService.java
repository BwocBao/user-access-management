package com.r2s.user.service;

import com.r2s.core.exception.ResourceNotFoundException;
import com.r2s.user.dto.UpdateUserRequest;
import com.r2s.user.dto.UserResponse;
import com.r2s.user.entity.UserProfile;
import com.r2s.user.mapper.UserMapper;
import com.r2s.user.messaging.EventPublisher;
import com.r2s.user.messaging.OutboxService;
import com.r2s.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserProfileRepository userRepository;
    private final UserMapper userMapper;
    private final OutboxService outboxService;

    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::toUserResponse)
                .toList();
    }

    public UserResponse getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(userMapper::toUserResponse)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    public UserResponse updateUser(UpdateUserRequest req, String username) {
        UserProfile user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        user.setFullName(req.getFullName());
        user.setEmail(req.getEmail());

        userRepository.save(user);
        return userMapper.toUserResponse(user);
    }

    @Transactional
    public void deleteUserByUsername(String username) {

        if (!userRepository.existsByUsername(username)) {
            throw new ResourceNotFoundException("User not found: " + username);
        }

        // Xóa profile trước rồi publish event xóa sang auth-service.
        // Flow này chấp nhận eventual consistency:
        // nếu publish thất bại, auth-service có thể chưa xóa user ngay.
        userRepository.deleteByUsername(username);
//        eventPublisher.publishUserDeleted(username);
        outboxService.saveUserDeletedEvent(username);
    }

    private UserProfile createUserProfileIfNotExists(String username) {
        try {
            UserProfile newUser = new UserProfile();
            newUser.setUsername(username);
            newUser.setFullName("");
            newUser.setEmail("");

            return userRepository.saveAndFlush(newUser);

        } catch (DataIntegrityViolationException e) {

            return userRepository.findByUsername(username)
                    .orElseThrow(() ->
                            new RuntimeException("Failed to create or find user", e));
        }
    }

}
