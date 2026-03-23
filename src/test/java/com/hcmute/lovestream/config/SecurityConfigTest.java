package com.hcmute.lovestream.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
public class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void whenUnauthenticated_thenRedirectOrUnauthorized() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    assertTrue(status == 302 || status == 401, "Expected 302 or 401 status but got " + status);
                });
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void givenUser_whenAccessAdmin_thenForbidden() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROLE_CONTENT_MANAGER")
    void givenContentManager_whenAccessAdminUsers_thenForbidden() throws Exception {
        mockMvc.perform(get("/admin/users/content-managers"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "ROLE_CONTENT_MANAGER")
    void givenContentManager_whenAccessAdminMovies_thenOk() throws Exception {
        mockMvc.perform(get("/admin/movies"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void givenAdmin_whenAccessAdminAny_thenOk() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/users/content-managers"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/movies"))
                .andExpect(status().isOk());
    }

    @Test
    void publicRoutes_whenUnauthenticated_thenOk() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }
}
