package com.esdc.gameapi.repository;

import com.esdc.gameapi.domain.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository for user entities.
 */
public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByNickname(String nickname);

  boolean existsByNickname(String nickname);
}
