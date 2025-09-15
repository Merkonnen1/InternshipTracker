package com.example.InternshipTracker;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Test
    @DisplayName("Anonymous user can access home page")
    void anonymous_canAccessHome() throws Exception {
        // Home page should be public
        mockMvc.perform(get("/"))
               .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Anonymous user is redirected to login for dashboard create")
    void anonymous_redirectedToLogin_onDashboardCreate() throws Exception {
        mockMvc.perform(get("/dashboard/create"))
               .andExpect(status().is3xxRedirection())
               .andExpect(redirectedUrlPattern("http://*/login"));
    }

    @Test
    @WithMockUser(username = "test-user", roles = {"USER"})
    @DisplayName("Authenticated user can access dashboard create")
    void authenticated_canAccessDashboardCreate() throws Exception {
        mockMvc.perform(get("/dashboard/create"))
               .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST to dashboard create without CSRF is forbidden")
    void postDashboardCreate_withoutCsrf_isForbidden() throws Exception {
        mockMvc.perform(
                    post("/dashboard/create")
                        // Intentionally no csrf()
                        .with(user("test-user").roles("USER"))
               )
               .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST to dashboard create with CSRF reaches controller (not 403)")
    void postDashboardCreate_withCsrf_notForbidden() throws Exception {
        mockMvc.perform(
                    post("/dashboard/create")
                        .with(user("test-user").roles("USER"))
                        .with(csrf())
                        // You can add form params here when your DTO is known, e.g.:
               )
               // We only assert it's not blocked by security; controller may return 200 (errors) or 3xx (redirect)
               .andExpect(result -> {
                   int status = result.getResponse().getStatus();
                   if (status == 403) {
                       throw new AssertionError("Expected not to be forbidden (403) when CSRF is present.");
                   }
               });
    }

    @Autowired
    private MockMvc mockMvc;
}