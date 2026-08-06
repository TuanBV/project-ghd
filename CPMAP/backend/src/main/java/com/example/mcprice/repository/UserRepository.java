package com.example.mcprice.repository;

import com.example.mcprice.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsernameAndEnabledTrue(String username);
}
