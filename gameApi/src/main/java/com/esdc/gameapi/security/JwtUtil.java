package com.esdc.gameapi.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * JWT token utility for generation, validation and claim extraction.
 */
@Slf4j
@Component
public class JwtUtil {

  @Value("${jwt.secret}")
  private String secret;

  @Value("${jwt.expiration}")
  private Long expiration;

  private SecretKey getSigningKey() {
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Extracts username from JWT token.
   */
  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  /**
   * Extracts token expiration date.
   */
  public Date extractExpiration(String token) {
    return extractClaim(token, Claims::getExpiration);
  }

  /**
   * Extracts custom claim from token.
   */
  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser()
        .verifyWith(getSigningKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  private Boolean isTokenExpired(String token) {
    return extractExpiration(token).before(new Date());
  }

  /**
   * Generates JWT token for user.
   */
  public String generateToken(String nickname, Long userId) {
    log.debug("Generating JWT token for user: {} (ID: {})", nickname, userId);
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);
    String token = createToken(claims, nickname);
    log.debug("JWT token generated successfully for user: {}", nickname);
    return token;
  }


  private String createToken(Map<String, Object> claims, String subject) {
    return Jwts.builder()
        .claims(claims)
        .subject(subject)
        .issuedAt(new Date(System.currentTimeMillis()))
        .expiration(new Date(System.currentTimeMillis() + expiration))
        .signWith(getSigningKey())
        .compact();
  }

  /**
   * Validates token against username.
   */
  public Boolean validateToken(String token, String nickname) {
    try {
      final String username = extractUsername(token);
      boolean valid = username.equals(nickname) && !isTokenExpired(token);
      log.debug("Token validation for {}: {}", nickname, valid ? "SUCCESS" : "FAILED");
      return valid;
    } catch (Exception e) {
      log.error("Token validation failed for {}: {}", nickname, e.getMessage());
      return false;
    }
  }
}
