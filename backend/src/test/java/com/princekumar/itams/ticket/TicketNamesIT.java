package com.princekumar.itams.ticket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.princekumar.itams.auth.dto.LoginRequest;
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
 * Ticket responses carry the names of the assignee and of each comment author,
 * so the UI no longer has to show "User #4".
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketNamesIT {

    @Container
    static final PostgreSQLContainer<?> pg =
        new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("itams_ticket_names").withUsername("itams").withPassword("itams");

    @DynamicPropertySource
    static void datasourceProps(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",      pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
        r.add("auth.rate-limit.max-attempts", () -> 10_000);
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;

    @Test
    void ticket_shows_assignee_and_comment_author_names() throws Exception {
        String employee = tokenFor("employee");
        String manager = tokenFor("itmanager");
        long managerId = userIdOf(manager);

        var created = mvc.perform(post("/api/v1/tickets").header("Authorization", "Bearer " + employee)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"subject\":\"Mouse broken\",\"description\":\"Left click does nothing\",\"priority\":\"LOW\",\"reporterPersonId\":1}"))
            .andExpect(status().isCreated())
            .andReturn();
        long ticketId = json.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(patch("/api/v1/tickets/{id}", ticketId).header("Authorization", "Bearer " + manager)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"assignedToUserId\":" + managerId + "}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assignedToName").value("IT Manager"));

        mvc.perform(post("/api/v1/tickets/{id}/comments", ticketId).header("Authorization", "Bearer " + employee)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"body\":\"Any news?\",\"internal\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.authorName").value("Regular Employee"));

        mvc.perform(get("/api/v1/tickets/{id}", ticketId).header("Authorization", "Bearer " + manager))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.assignedToName").value("IT Manager"))
            .andExpect(jsonPath("$.comments[0].authorName").value("Regular Employee"));
    }

    private long userIdOf(String token) throws Exception {
        var res = mvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andReturn();
        return json.readTree(res.getResponse().getContentAsString()).get("userId").asLong();
    }

    private String tokenFor(String username) throws Exception {
        var res = mvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(new LoginRequest(username, "changeme"))))
            .andExpect(status().isOk())
            .andReturn();
        return json.readTree(res.getResponse().getContentAsString()).get("accessToken").asText();
    }
}
