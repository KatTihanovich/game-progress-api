package com.esdc.gameapi.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "jwt.secret=mySecretKeyForTestingPurposesOnlyMustBeAtLeast256BitsLong",
    "jwt.expiration=3600000",
    "admin.password=testAdminPassword123"
})
@DisplayName("Security Integration Tests")
class SecurityIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  // ---------- PUBLIC ENDPOINTS ----------

  @Test
  @DisplayName("Public actuator health endpoint should be accessible without token")
  void shouldAllowAccessToHealthEndpoint() throws Exception {
    mockMvc.perform(get("/actuator/health"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("Public actuator info endpoint should be accessible without token")
  void shouldAllowAccessToInfoEndpoint() throws Exception {
    mockMvc.perform(get("/actuator/info"))
        .andExpect(status().isOk());
  }

  @Test
  @DisplayName("Register endpoint should not be blocked by security")
  void registerEndpointShouldBePublic() throws Exception {
    mockMvc.perform(get("/api/users/register"))
        .andExpect(status().isMethodNotAllowed());
  }

  @Test
  @DisplayName("Login endpoint should not be blocked by security")
  void loginEndpointShouldBePublic() throws Exception {
    mockMvc.perform(get("/api/users/login"))
        .andExpect(status().isMethodNotAllowed());
  }

  @Test
  @DisplayName("POST login with invalid credentials should return 400")
  void postLoginShouldReturn401ForInvalidCredentials() throws Exception {
    mockMvc.perform(
            post("/api/users/login")
                .contentType("application/json")
                .content("{}")
        )
        .andExpect(status().isBadRequest());
  }

  // ---------- PROTECTED ENDPOINTS ----------

  @Test
  @DisplayName("Protected endpoint without token should return 403")
  void shouldDenyAccessWithoutToken() throws Exception {
    mockMvc.perform(get("/api/progress"))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Protected endpoint with invalid JWT should return 401")
  void shouldRejectInvalidJwt() throws Exception {
    mockMvc.perform(get("/api/progress")
            .header("Authorization", "Bearer invalid.token.here"))
        .andExpect(status().isUnauthorized());
  }

  // ---------- EDGE CASES ----------

  @Test
  @DisplayName("Unknown protected endpoint should return 403")
  void unknownEndpointShouldReturn403() throws Exception {
    mockMvc.perform(get("/api/this/endpoint/does/not/exist"))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("OPTIONS request should not be blocked by security")
  void optionsRequestShouldBeAllowed() throws Exception {
    mockMvc.perform(options("/api/users/login"))
        .andExpect(status().isOk());
  }
}
