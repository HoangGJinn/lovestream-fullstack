package com.hcmute.lovestream.controller.web.admin;

import com.hcmute.lovestream.dto.request.admin.user.CreateContentManagerRequest;
import com.hcmute.lovestream.entity.User;
import com.hcmute.lovestream.entity.enums.Role;
import com.hcmute.lovestream.repository.UserRepository;
import com.hcmute.lovestream.service.admin.user.AdminUserManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private AdminUserManagementService adminUserManagementService;

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void givenAdmin_whenGetList_thenOk() throws Exception {
        User manager1 = User.builder().fullName("Manager 1").email("1@m.com").build();
        when(userRepository.findAllByRole(Role.CONTENT_MANAGER)).thenReturn(List.of(manager1));

        mockMvc.perform(get("/admin/users/content-managers"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/content-managers/list"))
                .andExpect(model().attributeExists("managers"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void givenAdmin_whenGetForm_thenOk() throws Exception {
        mockMvc.perform(get("/admin/users/content-managers/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/content-managers/form"))
                .andExpect(model().attributeExists("createContentManagerRequest"));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void givenAdminAndValidForm_whenPost_thenRedirectAndCreate() throws Exception {
        mockMvc.perform(post("/admin/users/content-managers")
                        .with(csrf()) // Must include CSRF for POST in Spring Security
                        .param("fullName", "New Manager")
                        .param("email", "manager@lovestream.com")
                        .param("phone", "0123456789")
                        .param("password", "strongpass")
                        .param("confirmPassword", "strongpass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users/content-managers"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(adminUserManagementService, times(1)).createContentManager(any(CreateContentManagerRequest.class));
    }

    @Test
    @WithMockUser(authorities = "ROLE_ADMIN")
    void givenAdminAndInvalidForm_whenPost_thenReturnForm() throws Exception {
        // Missing full name
        mockMvc.perform(post("/admin/users/content-managers")
                        .with(csrf())
                        .param("fullName", "") // Invalid blank name
                        .param("email", "manager@lovestream.com")
                        .param("phone", "0123456789")
                        .param("password", "strongpass")
                        .param("confirmPassword", "strongpass"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/content-managers/form"))
                .andExpect(model().hasErrors());

        verify(adminUserManagementService, never()).createContentManager(any());
    }

    @Test
    @WithMockUser(authorities = "ROLE_CONTENT_MANAGER")
    void givenManager_whenAccessAdminUsers_thenForbidden() throws Exception {
        mockMvc.perform(get("/admin/users/content-managers"))
                .andExpect(status().isForbidden());
    }
}
