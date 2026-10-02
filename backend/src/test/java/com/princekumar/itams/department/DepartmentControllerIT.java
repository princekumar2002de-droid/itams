package com.princekumar.itams.department;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.princekumar.itams.department.dto.DepartmentCreateRequest;
import com.princekumar.itams.department.dto.DepartmentUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "admin-test", roles = {"ADMIN"})
class DepartmentControllerIT {

    @Container
    static final PostgreSQLContainer<?> pg =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("itams_test").withUsername("itams").withPassword("itams");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",      pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired DepartmentRepository repo;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void wipe() { repo.deleteAll(); }

    @Test
    void create_and_update_write_audit_log_rows() throws Exception {
        // Regression test: audit inserts used to fail silently (jsonb column bound
        // as VARCHAR), so check that the rows really exist.
        String location = mvc.perform(post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new DepartmentCreateRequest("AUD", "Audit Dept", null, null))))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");
        long id = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));

        mvc.perform(patch("/api/v1/departments/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new DepartmentUpdateRequest("Audit Department", null, null))))
            .andExpect(status().isOk());

        Integer creates = jdbc.queryForObject(
            "SELECT count(*) FROM audit_log WHERE entity_type = 'Department' AND entity_id = ? AND action = 'CREATE'",
            Integer.class, id);
        Integer updates = jdbc.queryForObject(
            "SELECT count(*) FROM audit_log WHERE entity_type = 'Department' AND entity_id = ? AND action = 'UPDATE'",
            Integer.class, id);
        org.assertj.core.api.Assertions.assertThat(creates).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(updates).isEqualTo(1);
    }

    @Test
    void create_get_update_delete_flow() throws Exception {
        var createBody = json.writeValueAsString(
            new DepartmentCreateRequest("IT", "Information Technology", null, null));

        String location = mvc.perform(post("/api/v1/departments")
                .contentType(MediaType.APPLICATION_JSON).content(createBody))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(notNullValue()))
            .andExpect(jsonPath("$.code").value("IT"))
            .andReturn().getResponse().getHeader("Location");

        long id = Long.parseLong(location.substring(location.lastIndexOf('/') + 1));

        mvc.perform(get("/api/v1/departments/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("IT"));

        mvc.perform(get("/api/v1/departments").param("page", "0").param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1));

        mvc.perform(patch("/api/v1/departments/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new DepartmentUpdateRequest("IT Operations", null, null))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("IT Operations"))
            .andExpect(jsonPath("$.code").value("IT"));

        mvc.perform(delete("/api/v1/departments/{id}", id))
            .andExpect(status().isNoContent());

        mvc.perform(get("/api/v1/departments/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("resource.not_found"));
    }

    @Test
    void duplicate_code_returns_409() throws Exception {
        var body = json.writeValueAsString(new DepartmentCreateRequest("FIN", "Finance", null, null));
        mvc.perform(post("/api/v1/departments").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/departments").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("department.code_already_used"));
    }

    @Test
    void invalid_code_pattern_returns_400() throws Exception {
        var body = json.writeValueAsString(new DepartmentCreateRequest("lowercase-bad", "x", null, null));
        mvc.perform(post("/api/v1/departments").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("validation.failed"))
            .andExpect(jsonPath("$.details[0].field").value("code"));
    }

    @Test
    void missing_parent_returns_404() throws Exception {
        var body = json.writeValueAsString(new DepartmentCreateRequest("XYZ", "Test", 9_999L, null));
        mvc.perform(post("/api/v1/departments").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("resource.not_found"));
    }

    @Test
    void self_parent_returns_409() throws Exception {
        var create = json.writeValueAsString(new DepartmentCreateRequest("SELF", "Self", null, null));
        String loc = mvc.perform(post("/api/v1/departments").contentType(MediaType.APPLICATION_JSON).content(create))
            .andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        long id = Long.parseLong(loc.substring(loc.lastIndexOf('/') + 1));

        var patchBody = json.writeValueAsString(new DepartmentUpdateRequest(null, id, null));
        mvc.perform(patch("/api/v1/departments/{id}", id)
                .contentType(MediaType.APPLICATION_JSON).content(patchBody))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("department.self_parent"));
    }

    @Test
    void search_filters_by_query() throws Exception {
        mvc.perform(post("/api/v1/departments").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(new DepartmentCreateRequest("IT", "IT Ops", null, null))))
            .andExpect(status().isCreated());
        mvc.perform(post("/api/v1/departments").contentType(MediaType.APPLICATION_JSON)
            .content(json.writeValueAsString(new DepartmentCreateRequest("FIN", "Finance", null, null))))
            .andExpect(status().isCreated());

        mvc.perform(get("/api/v1/departments").param("q", "fin"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].code").value("FIN"));
    }

    @Test
    void health_endpoint_is_up() throws Exception {
        mvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("UP")));
    }
}
