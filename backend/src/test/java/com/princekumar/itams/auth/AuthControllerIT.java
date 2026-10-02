package com.princekumar.itams.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.princekumar.itams.auth.dto.LoginRequest;
import com.princekumar.itams.auth.dto.LogoutRequest;
import com.princekumar.itams.auth.dto.RefreshRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Auth flow tests — login, refresh, logout, /me — against a real Postgres
 * container with the seeded demo users from {@link com.princekumar.itams.config.DevBootstrap}.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIT {

    @Container
    static final PostgreSQLContainer<?> pg =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("itams_auth").withUsername("itams").withPassword("itams");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",      pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
        r.add("auth.rate-limit.max-attempts", () -> 10000);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    // ── login ────────────────────────────────────────────────────────────────

    @Test
    void login_success_returns_tokens() throws Exception {
        var res = mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new LoginRequest("admin", "changeme"))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken", not(emptyString())))
            .andExpect(jsonPath("$.refreshToken", not(emptyString())))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.expiresIn").value(900))
            .andReturn();

        JsonNode body = json.readTree(res.getResponse().getContentAsString());
        // Access token should look like a JWT (3 dot-separated Base64URL segments)
        String access = body.get("accessToken").asText();
        org.assertj.core.api.Assertions.assertThat(access.split("\\.")).hasSize(3);
    }

    @Test
    void login_wrong_password_returns_401_generic() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new LoginRequest("admin", "WRONG"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("auth.bad_credentials"))
            // Deliberately generic — no username enumeration.
            .andExpect(jsonPath("$.message").value("Invalid username or password."));
    }

    @Test
    void login_unknown_user_returns_401_generic() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new LoginRequest("no-such-user", "whatever"))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("auth.bad_credentials"));
    }

    @Test
    void login_missing_username_returns_400() throws Exception {
        mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"changeme\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("validation.failed"));
    }

    // ── /me ──────────────────────────────────────────────────────────────────

    @Test
    void me_with_valid_token_returns_current_user() throws Exception {
        String token = loginAs("admin", "changeme").get("accessToken").asText();
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("admin"))
            .andExpect(jsonPath("$.roles", hasItem("ROLE_ADMIN")));
    }

    @Test
    void me_without_token_returns_401() throws Exception {
        mvc.perform(get("/api/v1/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("auth.unauthenticated"));
    }

    @Test
    void me_with_garbage_token_returns_401() throws Exception {
        mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer not-a-real-jwt"))
            .andExpect(status().isUnauthorized());
    }

    // ── refresh + logout ─────────────────────────────────────────────────────

    @Test
    void refresh_rotates_the_refresh_token() throws Exception {
        JsonNode first = loginAs("admin", "changeme");
        String oldRefresh = first.get("refreshToken").asText();

        var res = mvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new RefreshRequest(oldRefresh))))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode second = json.readTree(res.getResponse().getContentAsString());
        String newRefresh = second.get("refreshToken").asText();
        org.assertj.core.api.Assertions.assertThat(newRefresh).isNotEqualTo(oldRefresh);

        // The old refresh token is now revoked and cannot be reused.
        mvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new RefreshRequest(oldRefresh))))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("auth.bad_credentials"));
    }

    @Test
    void reusing_a_rotated_refresh_token_revokes_the_whole_family() throws Exception {
        String t1 = loginAs("admin", "changeme").get("refreshToken").asText();
        String t2 = refresh(t1).get("refreshToken").asText();      // legit rotation t1 -> t2

        // Replay of t1 (e.g. stolen copy): rejected ...
        mvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new RefreshRequest(t1))))
            .andExpect(status().isUnauthorized());

        // ... and the still-active t2 of the same family is now dead too.
        // This proves the revocation was COMMITTED despite the 401 (noRollbackFor).
        mvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new RefreshRequest(t2))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void reuse_in_one_session_does_not_log_out_another_session() throws Exception {
        String sessionA = loginAs("admin", "changeme").get("refreshToken").asText();
        String sessionB = loginAs("admin", "changeme").get("refreshToken").asText();

        refresh(sessionA);                                          // rotate A
        mvc.perform(post("/api/v1/auth/refresh")                    // replay old A -> family A revoked
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new RefreshRequest(sessionA))))
            .andExpect(status().isUnauthorized());

        refresh(sessionB);                                          // B is a different family: still works
    }

    @Test
    void logout_revokes_refresh_token() throws Exception {
        String refresh = loginAs("admin", "changeme").get("refreshToken").asText();

        mvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new LogoutRequest(refresh))))
            .andExpect(status().isNoContent());

        mvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new RefreshRequest(refresh))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_unknown_token_is_idempotent() throws Exception {
        mvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new LogoutRequest("garbage"))))
            .andExpect(status().isNoContent());
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private JsonNode refresh(String refreshToken) throws Exception {
        var res = mvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new RefreshRequest(refreshToken))))
            .andExpect(status().isOk())
            .andReturn();
        return json.readTree(res.getResponse().getContentAsString());
    }

    private JsonNode loginAs(String user, String password) throws Exception {
        var res = mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new LoginRequest(user, password))))
            .andExpect(status().isOk())
            .andReturn();
        return json.readTree(res.getResponse().getContentAsString());
    }
}
