package com.esdc.gameapi.repository;

import com.esdc.gameapi.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByNickname(String nickname);

  boolean existsByNickname(String nickname);
}
