package com.r2s.user.repository;

import com.r2s.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile,Long> {
    Optional<UserProfile> findByUsername(String username);
    void deleteByUsername(String username);

    boolean existsByUsername(String username);
}
