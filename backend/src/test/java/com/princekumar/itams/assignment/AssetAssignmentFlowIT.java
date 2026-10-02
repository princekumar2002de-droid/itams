package com.princekumar.itams.assignment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.princekumar.itams.asset.dto.AssetCreateRequest;
import com.princekumar.itams.asset.dto.AssetModelCreateRequest;
import com.princekumar.itams.assignment.dto.AssignAssetRequest;
import com.princekumar.itams.assignment.dto.ReturnAssetRequest;
import com.princekumar.itams.department.dto.DepartmentCreateRequest;
import com.princekumar.itams.employee.dto.EmployeeCreateRequest;
import com.princekumar.itams.person.dto.PersonCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "admin-test", roles = {"ADMIN"})
class AssetAssignmentFlowIT {

    @Container
    static final PostgreSQLContainer<?> pg =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("itams_e2e").withUsername("itams").withPassword("itams");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",      pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void full_lifecycle() throws Exception {
        long deptId = created(post("/api/v1/departments"),
            new DepartmentCreateRequest("ENG-FLOW", "Engineering", null, null));

        long personId = created(post("/api/v1/people"),
            new PersonCreateRequest("Grace", "Hopper", "grace-flow@example.com", null));

        created(post("/api/v1/employees"),
            new EmployeeCreateRequest(personId, "EMP-FLOW-01", deptId, "Rear Admiral", LocalDate.of(2026, 1, 15)));

        long categoryId = 1L;

        long modelId = created(post("/api/v1/asset-models"),
            new AssetModelCreateRequest(categoryId, "Lenovo", "ThinkPad T14 Gen 4 flow", null));

        long assetId = created(post("/api/v1/assets"),
            new AssetCreateRequest("A-FLOW-01", modelId, "SN-FLOW-1234",
                LocalDate.of(2026, 3, 1), new BigDecimal("1500.00"),
                LocalDate.of(2029, 3, 1), null));

        long assignmentId = created(post("/api/v1/assignments"),
            new AssignAssetRequest(assetId, personId, LocalDate.of(2027, 1, 1),
                AssetCondition.NEW, "First laptop"));

        mvc.perform(get("/api/v1/assets/{id}", assetId))
           .andExpect(jsonPath("$.status").value("ASSIGNED"));

        mvc.perform(post("/api/v1/assignments").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(
                    new AssignAssetRequest(assetId, personId, null, AssetCondition.NEW, null))))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.code").value("assignment.asset_not_in_stock"));

        mvc.perform(post("/api/v1/assignments/{id}/return", assignmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new ReturnAssetRequest(AssetCondition.GOOD, false, "OK"))))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.open").value(false))
           .andExpect(jsonPath("$.inCondition").value("GOOD"));

        mvc.perform(get("/api/v1/assets/{id}", assetId))
           .andExpect(jsonPath("$.status").value("IN_STOCK"));

        mvc.perform(post("/api/v1/assets/{id}/retire", assetId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"End of life\"}"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("RETIRED"));
    }

    @Test
    void maintenance_loop_returns_asset_to_stock() throws Exception {
        long personId = created(post("/api/v1/people"),
            new PersonCreateRequest("Ada", "Lovelace", "ada-flow@example.com", null));
        long modelId = created(post("/api/v1/asset-models"),
            new AssetModelCreateRequest(1L, "Dell", "Latitude 7440 flow", null));
        long assetId = created(post("/api/v1/assets"),
            new AssetCreateRequest("A-FLOW-50", modelId, "SN-FLOW-5050",
                LocalDate.of(2026, 2, 1), new BigDecimal("1200.00"), null, null));
        long assignmentId = created(post("/api/v1/assignments"),
            new AssignAssetRequest(assetId, personId, null, AssetCondition.GOOD, null));

        // Return it broken → goes to maintenance, cannot be assigned
        mvc.perform(post("/api/v1/assignments/{id}/return", assignmentId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new ReturnAssetRequest(AssetCondition.POOR, true, "Screen cracked"))))
           .andExpect(status().isOk());
        mvc.perform(get("/api/v1/assets/{id}", assetId))
           .andExpect(jsonPath("$.status").value("UNDER_MAINTENANCE"));
        mvc.perform(post("/api/v1/assignments").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new AssignAssetRequest(assetId, personId, null, AssetCondition.GOOD, null))))
           .andExpect(status().isConflict());

        // Repair done → back in stock
        mvc.perform(post("/api/v1/assets/{id}/maintenance-complete", assetId))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("IN_STOCK"));

        // Completing twice is a business-rule violation, not a 500
        mvc.perform(post("/api/v1/assets/{id}/maintenance-complete", assetId))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.code").value("asset.not_under_maintenance"));
    }

    @Test
    void cannot_assign_retired_asset() throws Exception {
        long personId = created(post("/api/v1/people"),
            new PersonCreateRequest("Alan", "Turing", "alan-flow@example.com", null));
        long modelId = created(post("/api/v1/asset-models"),
            new AssetModelCreateRequest(1L, "Apple", "MacBook Air M3 flow", null));
        long assetId = created(post("/api/v1/assets"),
            new AssetCreateRequest("A-FLOW-99", modelId, "SN-FLOW-9999",
                LocalDate.of(2026, 1, 1), new BigDecimal("999.00"), null, null));

        mvc.perform(post("/api/v1/assets/{id}/retire", assetId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Test\"}"))
           .andExpect(status().isOk());

        mvc.perform(post("/api/v1/assignments").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(
                    new AssignAssetRequest(assetId, personId, null, AssetCondition.NEW, null))))
           .andExpect(status().isConflict())
           .andExpect(jsonPath("$.code").value("assignment.asset_not_in_stock"));
    }

    private long created(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder req,
                         Object body) throws Exception {
        String location = mvc.perform(req.contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }
}
