package com.hcmute.lovestream.controller.web.admin;

import com.hcmute.lovestream.entity.Genre;
import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.entity.enums.AgeRating;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.entity.enums.Quality;
import com.hcmute.lovestream.repository.GenreRepository;
import com.hcmute.lovestream.service.admin.movie.AdminMovieManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    // Fix pre-existing issue: Hibernate JOINED inheritance triggers GlobalTemporaryTableStrategy
    // which tries to connect to prod MySQL during H2 test context boot. Inline strategy avoids this.
    "spring.jpa.properties.hibernate.query.mutation_strategy=org.hibernate.query.sqm.mutation.internal.inline.InlineMutationStrategy"
})
@AutoConfigureMockMvc
public class AdminMovieControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminMovieManagementService adminMovieManagementService;

    @MockitoBean
    private GenreRepository genreRepository;

    private void mockAttributes() {
        when(genreRepository.findAll()).thenReturn(List.of(new Genre("genre-id", "Action", List.of())));
    }

    @Test
    @WithMockUser(roles = "USER")
    void givenUser_whenAccessAdminMovies_thenForbidden() throws Exception {
        mockMvc.perform(get("/admin/movies"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CONTENT_MANAGER")
    void givenContentManager_whenGetList_thenOk() throws Exception {
        mockAttributes();
        when(adminMovieManagementService.filterMovies(null, null)).thenReturn(List.of(new Movie()));

        mockMvc.perform(get("/admin/movies"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/movies/list"))
                .andExpect(model().attributeExists("movies"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void givenAdmin_whenGetForm_thenOk() throws Exception {
        mockAttributes();

        mockMvc.perform(get("/admin/movies/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/movies/form"))
                .andExpect(model().attributeExists("movieUpsertRequest"))
                .andExpect(model().attributeExists("allGenres"))
                .andExpect(model().attributeExists("allAgeRatings"));
    }

    @Test
    @WithMockUser(roles = "CONTENT_MANAGER")
    void givenManagerAndValidForm_whenPostCreate_thenRedirectToEdit() throws Exception {
        // op1 flow: sau khi tạo thành công phải redirect sang trang edit để upload asset
        Movie created = new Movie();
        created.setId("mock-id");
        when(adminMovieManagementService.createMovie(any())).thenReturn(created);

        mockMvc.perform(post("/admin/movies")
                        .with(csrf())
                        .param("title", "Avatar 3")
                        .param("description", "Desc")
                        .param("releaseYear", "2024")
                        .param("releaseDate", "2024-12-12")
                        .param("durationMinutes", "190")
                        .param("ageRating", "PG_13")
                        .param("quality", "HD")
                        .param("status", "ACTIVE")
                        .param("genreIds", "genre-123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/movies/mock-id/edit"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void givenAdminAndInvalidForm_whenPostCreate_thenReturnForm() throws Exception {
        mockAttributes();
        // Cố tình đẩy Form rỗng (thiếu Title, releaseDate, vv...)
        mockMvc.perform(post("/admin/movies")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/movies/form"))
                // Model lúc này phải hứng được báo lỗi từ Hibernate Validator
                .andExpect(model().attributeHasFieldErrors("movieUpsertRequest", "title"));

        verify(adminMovieManagementService, never()).createMovie(any());
    }

    @Test
    @WithMockUser(roles = "CONTENT_MANAGER")
    void givenMissingGenreIds_whenPostCreate_thenReturnFormWithGenreError() throws Exception {
        mockAttributes();
        // Gửi form hợp lệ nhưng thiếu genreIds → @NotEmpty phải bắt lỗi
        mockMvc.perform(post("/admin/movies")
                        .with(csrf())
                        .param("title", "Avatar 3")
                        .param("description", "Desc")
                        .param("releaseYear", "2024")
                        .param("releaseDate", "2024-12-12")
                        .param("durationMinutes", "190")
                        .param("ageRating", "PG_13")
                        .param("quality", "HD")
                        .param("status", "ACTIVE"))
                // Không gửi genreIds
                .andExpect(status().isOk())
                .andExpect(view().name("admin/movies/form"))
                .andExpect(model().attributeHasFieldErrors("movieUpsertRequest", "genreIds"));

        verify(adminMovieManagementService, never()).createMovie(any());
    }

    @Test
    @WithMockUser(roles = "CONTENT_MANAGER")
    void givenManagerAndValidForm_whenPostUpdate_thenRedirectToEdit() throws Exception {
        // op1 flow: sau khi update phải redirect về lại trang edit (không về list)
        when(adminMovieManagementService.updateMovie(anyString(), any())).thenReturn(new Movie());

        mockMvc.perform(post("/admin/movies/m-1")
                        .with(csrf())
                        .param("title", "Avatar 3 Updated")
                        .param("description", "Desc updated")
                        .param("releaseYear", "2024")
                        .param("releaseDate", "2024-12-12")
                        .param("durationMinutes", "195")
                        .param("ageRating", "PG_13")
                        .param("quality", "UHD_4K")
                        .param("status", "ACTIVE")
                        .param("genreIds", "genre-123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/movies/m-1/edit"))
                .andExpect(flash().attributeExists("successMessage"));
    }

    @Test
    @WithMockUser(roles = "CONTENT_MANAGER")
    void givenAdmin_whenGetNewForm_thenModelHasNullId() throws Exception {
        // Trang tạo mới: movieUpsertRequest.id phải null → template ẩn upload section
        mockAttributes();

        mockMvc.perform(get("/admin/movies/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/movies/form"))
                .andExpect(model().attribute("movieUpsertRequest",
                        org.hamcrest.Matchers.hasProperty("id", org.hamcrest.Matchers.nullValue())));
    }

    @Test
    @WithMockUser(roles = "CONTENT_MANAGER")
    void givenManager_whenGetEditForm_thenModelHasId() throws Exception {
        // Trang edit: movieUpsertRequest.id phải có giá trị → template hiển thị upload section
        mockAttributes();

        Movie fakeMovie = new Movie();
        fakeMovie.setId("m-1");
        fakeMovie.setTitle("Old Movie");
        fakeMovie.setGenres(Set.of());
        when(adminMovieManagementService.getMovieById("m-1")).thenReturn(fakeMovie);

        mockMvc.perform(get("/admin/movies/m-1/edit"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/movies/form"))
                .andExpect(model().attribute("movieUpsertRequest",
                        org.hamcrest.Matchers.hasProperty("id", org.hamcrest.Matchers.is("m-1"))));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void givenAdmin_whenPostHideMovie_thenRedirect() throws Exception {
        doNothing().when(adminMovieManagementService).hideMovie("m-1");

        mockMvc.perform(post("/admin/movies/m-1/hide")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/movies"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(adminMovieManagementService, times(1)).hideMovie("m-1");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void givenAdmin_whenPostRestoreMovie_thenRedirect() throws Exception {
        doNothing().when(adminMovieManagementService).restoreMovie("m-1");

        mockMvc.perform(post("/admin/movies/m-1/restore")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/movies"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(adminMovieManagementService, times(1)).restoreMovie("m-1");
    }

    // --- Security tests for hide/restore (Task 14) ---

    @Test
    @WithMockUser(roles = "USER")
    void givenUser_whenPostHideMovie_thenForbidden() throws Exception {
        mockMvc.perform(post("/admin/movies/m-1/hide")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(adminMovieManagementService, never()).hideMovie(any());
    }

    @Test
    @WithMockUser(roles = "USER")
    void givenUser_whenPostRestoreMovie_thenForbidden() throws Exception {
        mockMvc.perform(post("/admin/movies/m-1/restore")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(adminMovieManagementService, never()).restoreMovie(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void givenAdmin_whenHideNonExistentMovie_thenRedirectWithError() throws Exception {
        doThrow(new RuntimeException("Không tìm thấy phim lẻ với ID: bad-id"))
                .when(adminMovieManagementService).hideMovie("bad-id");

        mockMvc.perform(post("/admin/movies/bad-id/hide")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/movies"))
                .andExpect(flash().attributeExists("errorMessage"));
    }
}
