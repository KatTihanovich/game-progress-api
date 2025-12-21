package com.esdc.gameapi.security;

import com.esdc.gameapi.domain.entity.User;
import com.esdc.gameapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Custom UserDetailsService for loading users by nickname.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String nickname) throws UsernameNotFoundException {
    User user = userRepository.findByNickname(nickname)
        .orElseThrow(() -> new UsernameNotFoundException("User not found: " + nickname));
    return new CustomUserDetails(user);
  }
}
