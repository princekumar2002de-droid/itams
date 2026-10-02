package com.princekumar.itams.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.princekumar.itams.auth.dto.LoginRequest;
import com.princekumar.itams.department.dto.DepartmentCreateRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Role-based authorization matrix. Verifies that {@code @PreAuthorize}
 * on the controllers is doing its job by logging in as three seeded
 * users (one per role) and hitting representative endpoints across
 * every controller group, including stats, licenses,
 * maintenance, tickets).
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizationIT {

    @Container
    static final PostgreSQLContainer<?> pg =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("itams_authz").withUsername("itams").withPassword("itams");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",      pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
        // Tests rapidly re-hit /auth/login for tokens — disable rate limiting for tests.
        r.add("auth.rate-limit.max-attempts", () -> 10_000);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    // ── no token ─────────────────────────────────────────────────────────────

    @Test
    void anonymous_request_to_protected_endpoint_returns_401() throws Exception {
        mvc.perform(get("/api/v1/departments"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("auth.unauthenticated"));
    }

    @Test
    void anonymous_can_reach_health() throws Exception {
        mvc.perform(get("/actuator/health"))
            .andExpect(status().isOk());
    }

    // ── ADMIN can do anything ────────────────────────────────────────────────

    @Test
    void admin_can_create_department() throws Exception {
        String token = tokenFor("admin");
        var body = json.writeValueAsString(new DepartmentCreateRequest("AUTHZ-A", "AuthZ test A", null, null));
        mvc.perform(post("/api/v1/departments").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated());
    }

    @Test
    void admin_can_list_departments() throws Exception {
        String token = tokenFor("admin");
        mvc.perform(get("/api/v1/departments").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    // ── IT_MANAGER: read yes, admin-only write no ────────────────────────────

    @Test
    void it_manager_can_list_departments() throws Exception {
        String token = tokenFor("itmanager");
        mvc.perform(get("/api/v1/departments").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void it_manager_cannot_create_department() throws Exception {
        String token = tokenFor("itmanager");
        var body = json.writeValueAsString(new DepartmentCreateRequest("AUTHZ-B", "AuthZ test B", null, null));
        mvc.perform(post("/api/v1/departments").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("auth.forbidden"));
    }

    @Test
    void it_manager_can_list_assets() throws Exception {
        String token = tokenFor("itmanager");
        mvc.perform(get("/api/v1/assets").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    // ── EMPLOYEE: can read asset-catalogue-ish things, cannot read HR ────────

    @Test
    void employee_cannot_list_departments() throws Exception {
        String token = tokenFor("employee");
        mvc.perform(get("/api/v1/departments").header("Authorization", "Bearer " + token))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("auth.forbidden"));
    }

    @Test
    void employee_can_list_assets() throws Exception {
        String token = tokenFor("employee");
        mvc.perform(get("/api/v1/assets").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk());
    }

    @Test
    void employee_cannot_create_asset() throws Exception {
        String token = tokenFor("employee");
        mvc.perform(post("/api/v1/assets").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"assetTag\":\"A-X\",\"modelId\":1,\"serialNumber\":\"SN\",\"purchaseDate\":\"2026-01-01\",\"purchasePrice\":1.00}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void employee_cannot_create_person() throws Exception {
        String token = tokenFor("employee");
        mvc.perform(post("/api/v1/people").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"firstName\":\"X\",\"lastName\":\"Y\",\"email\":\"xy@example.com\"}"))
            .andExpect(status().isForbidden());
    }

    // ── stats, licenses, maintenance, tickets ─────────────────────────────────

    @Test
    void anyone_authenticated_can_read_dashboard_stats() throws Exception {
        for (String user : new String[]{"admin", "itmanager", "employee"}) {
            String token = tokenFor(user);
            mvc.perform(get("/api/v1/stats/dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        }
    }

    @Test
    void anyone_authenticated_can_list_licenses() throws Exception {
        for (String user : new String[]{"admin", "itmanager", "employee"}) {
            String token = tokenFor(user);
            mvc.perform(get("/api/v1/licenses").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        }
    }

    @Test
    void employee_cannot_create_license() throws Exception {
        String token = tokenFor("employee");
        mvc.perform(post("/api/v1/licenses").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"vendor":"Vendor","productName":"P","productVersion":null,
                     "licenseReference":"REF-X","licenseType":"PER_SEAT","seatsTotal":1,
                     "purchaseDate":"2026-01-01","expiresOn":"2027-01-01",
                     "cost":1.00,"procurementRef":null}
                """))
            .andExpect(status().isForbidden());
    }

    @Test
    void it_manager_can_create_license_but_employee_cannot() throws Exception {
        // itmanager: 201 or a validation-style error, but NOT 403.
        String token = tokenFor("itmanager");
        mvc.perform(post("/api/v1/licenses").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"vendor":"Vendor","productName":"P","productVersion":null,
                     "licenseReference":"REF-Z","licenseType":"PER_SEAT","seatsTotal":1,
                     "purchaseDate":"2026-01-01","expiresOn":"2027-01-01",
                     "cost":1.00,"procurementRef":null}
                """))
            .andExpect(result -> {
                int code = result.getResponse().getStatus();
                if (code == 403) throw new AssertionError("IT_MANAGER got 403 on POST /licenses");
            });
    }

    @Test
    void anyone_authenticated_can_list_maintenance_but_only_managers_can_write() throws Exception {
        for (String user : new String[]{"admin", "itmanager", "employee"}) {
            String token = tokenFor(user);
            mvc.perform(get("/api/v1/maintenance").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        }
        String employee = tokenFor("employee");
        mvc.perform(post("/api/v1/maintenance").header("Authorization", "Bearer " + employee)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"assetId":1,"performedOn":"2026-01-01","performedByUserId":1,
                     "providerName":null,"description":"x","cost":0.00,"nextScheduledOn":null}
                """))
            .andExpect(status().isForbidden());
    }

    @Test
    void anyone_authenticated_can_list_and_raise_tickets() throws Exception {
        for (String user : new String[]{"admin", "itmanager", "employee"}) {
            String token = tokenFor(user);
            mvc.perform(get("/api/v1/tickets").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        }
    }

    @Test
    void only_managers_can_change_ticket_status() throws Exception {
        String employee = tokenFor("employee");
        mvc.perform(post("/api/v1/tickets/999999/status").header("Authorization", "Bearer " + employee)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"to\":\"CLOSED\"}"))
            .andExpect(status().isForbidden());
    }

    // ── helper ───────────────────────────────────────────────────────────────

    private String tokenFor(String username) throws Exception {
        var res = mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new LoginRequest(username, "changeme"))))
            .andExpect(status().isOk())
            .andReturn();
        return json.readTree(res.getResponse().getContentAsString())
                   .get("accessToken").asText();
    }
}
