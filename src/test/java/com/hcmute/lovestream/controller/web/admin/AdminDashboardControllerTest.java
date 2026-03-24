package com.hcmute.lovestream.controller.web.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void givenAdmin_whenAccessDashboard_thenSeeBothCards() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Quản lý Content Manager")))
                .andExpect(content().string(containsString("Quản lý phim lẻ")))
                .andExpect(content().string(containsString("Tổng Doanh Thu")));
    }

    @Test
    @WithMockUser(authorities = "ROLE_CONTENT_MANAGER")
    void givenContentManager_whenAccessDashboard_thenSeeOnlyMovieCard() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Quản lý Content Manager"))))
                .andExpect(content().string(containsString("Quản lý phim lẻ")))
                .andExpect(content().string(not(containsString("Tổng Doanh Thu"))));
    }

    @Test
    @WithMockUser(authorities = "ROLE_USER")
    void givenUser_whenAccessDashboard_thenForbidden() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    void givenUnauthenticated_whenAccessAdminRedirect_thenRedirectToLogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(status == 302 || status == 401);
                });
    }
}
