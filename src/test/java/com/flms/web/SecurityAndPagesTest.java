package com.flms.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.seed-data=false")
@AutoConfigureMockMvc
class SecurityAndPagesTest {

    @Autowired
    MockMvc mvc;

    @Test
    void anonymousUserIsSentToLogin() throws Exception {
        mvc.perform(get("/leaves")).andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void healthEndpointIsPublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @WithMockUser(username = "faculty1", roles = "FACULTY")
    void facultyCannotApprove() throws Exception {
        mvc.perform(post("/leaves/1/approve").with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "faculty1", roles = "FACULTY")
    void invalidDateRangeShowsFormError() throws Exception {
        mvc.perform(post("/leaves").with(csrf())
                .param("leaveType", "CASUAL")
                .param("startDate", "2026-11-10")
                .param("endDate", "2026-11-09")
                .param("reason", "Family function"))
            .andExpect(status().isOk())
            .andExpect(model().attributeHasFieldErrors("leaveForm", "dateRangeValid"))
            .andExpect(content().string(containsString("End date cannot be before start date")));
    }

    @Test
    @WithMockUser(username = "faculty1", roles = "FACULTY")
    void validApplicationRedirectsToDetails() throws Exception {
        mvc.perform(post("/leaves").with(csrf())
                .param("leaveType", "SICK")
                .param("startDate", "2026-11-10")
                .param("endDate", "2026-11-10")
                .param("reason", "Dental appointment"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("/leaves/*"));
    }

    @Test
    @WithMockUser(username = "hod", roles = "HOD")
    void summaryApiReturnsJson() throws Exception {
        mvc.perform(get("/api/summary")).andExpect(status().isOk())
            .andExpect(jsonPath("$.pending").isNumber());
    }

    @Test
    @WithMockUser(username = "faculty1", roles = "FACULTY")
    void missingRecordIs404() throws Exception {
        mvc.perform(get("/leaves/99999")).andExpect(status().isNotFound());
    }
}
