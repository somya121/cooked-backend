package com.cookedapp.cooked_backend.repository;

import com.cookedapp.cooked_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);

    Optional<User> findBySetupToken(String setupToken);
}