package com.nurbb.libris.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StatisticsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = {"LIBRARIAN"})
    void shouldReturnLibraryStatisticsAsText() throws Exception {
        mockMvc.perform(get("/api/statistics/text-report")
                        .accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(containsString("LIBRARY STATISTICS REPORT")));
    }

    @Test
    @WithMockUser(roles = {"LIBRARIAN"})
    void shouldReturnOverdueStatistics() throws Exception {
        mockMvc.perform(get("/api/statistics/overdue")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBorrows").exists())
                .andExpect(jsonPath("$.overdueBorrows").exists())
                .andExpect(jsonPath("$.overdueRatio").exists());
    }

    @Test
    @WithMockUser(roles = {"PATRON"})
    void shouldReturnForbiddenForUnauthorizedRole() throws Exception {
        mockMvc.perform(get("/api/statistics/text-report"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/statistics/overdue"))
                .andExpect(status().isForbidden());
    }
}
