package com.hcmute.lovestream.service.admin.movie;

import com.hcmute.lovestream.dto.request.admin.movie.MovieUpsertRequest;
import com.hcmute.lovestream.entity.Genre;
import com.hcmute.lovestream.entity.MediaAsset;
import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.entity.enums.AssetType;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.entity.enums.Quality;
import com.hcmute.lovestream.entity.enums.AgeRating;
import com.hcmute.lovestream.repository.GenreRepository;
import com.hcmute.lovestream.repository.MediaAssetRepository;
import com.hcmute.lovestream.repository.MovieRepository;
import com.hcmute.lovestream.service.admin.movie.impl.AdminMovieManagementServiceImpl;
import com.hcmute.lovestream.service.storage.MediaStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AdminMovieManagementServiceTest {

    @Mock
    private MovieRepository movieRepository;

    @Mock
    private GenreRepository genreRepository;

    @Mock
    private MediaAssetRepository mediaAssetRepository;

    @Mock
    private MediaStorageService mediaStorageService; // Added in Task 13 as required field

    @InjectMocks
    private AdminMovieManagementServiceImpl adminMovieManagementService;

    // --- Mock Data Helpers ---
    private MovieUpsertRequest createMockRequest() {
        return MovieUpsertRequest.builder()
                .title("Avenger Endgame")
                .description("Một bộ phim bom tấn nổi bật")
                .releaseYear(2019)
                .releaseDate(LocalDate.of(2019, 4, 26))
                .durationMinutes(181)
                .ageRating(AgeRating.PG_13)
                .quality(Quality.HD)
                .status(ContentStatus.ACTIVE)
                .genreIds(List.of("genre-1"))
                .build();
    }

    private Genre createMockGenre() {
        Genre genre = new Genre();
        genre.setId("genre-1");
        genre.setName("Hành động");
        return genre;
    }

    private Movie createMockMovie() {
        Movie movie = new Movie();
        movie.setId("movie-1");
        movie.setTitle("Old Title");
        movie.setStatus(ContentStatus.HIDDEN);
        return movie;
    }

    // --- TESTS ---

    @Test
    void whenCreateMovie_Success() {
        // Arrange
        MovieUpsertRequest request = createMockRequest();

        when(genreRepository.findAllById(anyList())).thenReturn(List.of(createMockGenre()));

        // Chặn luồng save, trả về Movie mới giả vờ là đã được cấp UUID
        when(movieRepository.save(any(Movie.class))).thenAnswer(invocation -> {
            Movie savedMovie = invocation.getArgument(0);
            savedMovie.setId("mock-new-uuid");
            return savedMovie;
        });

        // Act
        Movie result = adminMovieManagementService.createMovie(request);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("mock-new-uuid");
        assertThat(result.getTitle()).isEqualTo("Avenger Endgame");
        assertThat(result.getDurationMinutes()).isEqualTo(181);
        assertThat(result.getReleaseDate().toString()).isEqualTo("2019-04-26");
        assertThat(result.getGenres()).hasSize(1);
        
        verify(movieRepository, times(1)).save(any(Movie.class));
    }

    @Test
    void whenCreateMovie_WithInvalidGenreId_ThrowsException() {
        // Arrange
        MovieUpsertRequest request = createMockRequest();
        // Giả lập Repository tìm mảng Genre bằng danh sách RỖNG (Không tìm thấy)
        when(genreRepository.findAllById(anyList())).thenReturn(List.of());

        // Act & Assert
        assertThatThrownBy(() -> adminMovieManagementService.createMovie(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Có ít nhất một thể loại không tồn tại trong hệ thống");

        verify(movieRepository, never()).save(any(Movie.class));
    }

    @Test
    void whenUpdateMovie_Success() {
        // Arrange
        MovieUpsertRequest request = createMockRequest();
        request.setTitle("Avenger Updated");

        Movie existingMovie = createMockMovie(); // Fake ID: movie-1, Title: Old Title

        when(movieRepository.findById("movie-1")).thenReturn(Optional.of(existingMovie));
        when(genreRepository.findAllById(anyList())).thenReturn(List.of(createMockGenre()));

        when(movieRepository.save(any(Movie.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        Movie result = adminMovieManagementService.updateMovie("movie-1", request);

        // Assert
        assertThat(result.getId()).isEqualTo("movie-1"); // Đảm bảo không đổi ID 
        assertThat(result.getTitle()).isEqualTo("Avenger Updated"); // Đảm bảo title đã override
        assertThat(existingMovie.getTitle()).isEqualTo("Avenger Updated"); // Bản reference gốc cũng bị đổi chứng tỏ Mapping chuẩn
        
        verify(movieRepository, times(1)).save(existingMovie);
    }

    @Test
    void whenUpdateOrHideMovie_AndNotFound_ThrowsException() {
        // Arrange
        when(movieRepository.findById("invalid-id")).thenReturn(Optional.empty());

        // Act & Assert Create
        assertThatThrownBy(() -> adminMovieManagementService.updateMovie("invalid-id", createMockRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không tìm thấy phim lẻ");

        // Act & Assert Hide
        assertThatThrownBy(() -> adminMovieManagementService.hideMovie("invalid-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không tìm thấy phim lẻ");

        verify(movieRepository, never()).save(any());
    }

    @Test
    void whenHideAndRestoreMovie_Success() {
        // Arrange (Test gộp 2 action Hide Restore vì thao tác giống hệt nhau về flow)
        Movie movie = createMockMovie();
        when(movieRepository.findById("movie-1")).thenReturn(Optional.of(movie));

        // Act Hide
        adminMovieManagementService.hideMovie("movie-1");
        assertThat(movie.getStatus()).isEqualTo(ContentStatus.HIDDEN);

        // Act Restore
        adminMovieManagementService.restoreMovie("movie-1");
        assertThat(movie.getStatus()).isEqualTo(ContentStatus.ACTIVE);

        verify(movieRepository, times(2)).save(movie); // Được gọi lưu 2 lần
    }

    @Test
    void whenAddAsset_Success() {
        // Arrange
        Movie movie = createMockMovie();
        when(movieRepository.findById("movie-1")).thenReturn(Optional.of(movie));
        when(mediaAssetRepository.save(any(MediaAsset.class))).thenAnswer(i -> {
            MediaAsset asset = i.getArgument(0);
            asset.setId("asset-1");
            return asset;
        });

        // Act
        MediaAsset result = adminMovieManagementService.addAsset("movie-1", AssetType.POSTER, "http://image.jpg");

        // Assert
        assertThat(result.getId()).isEqualTo("asset-1");
        assertThat(result.getAssetType()).isEqualTo(AssetType.POSTER);
        assertThat(result.getAssetUrl()).isEqualTo("http://image.jpg");
        assertThat(result.getVideoContent()).isEqualTo(movie); // Validate the relationship is established
    }

    @Test
    void whenRemoveAsset_NotBelongToMovie_ThrowsException() {
        // Arrange
        Movie movie = createMockMovie(); 
        movie.setId("movie-1");

        Movie anotherMovie = new Movie(); 
        anotherMovie.setId("movie-2");

        MediaAsset asset = new MediaAsset();
        asset.setId("asset-1");
        asset.setVideoContent(anotherMovie); // Gắn cọc cho Củ Cải 2

        when(movieRepository.findById("movie-1")).thenReturn(Optional.of(movie));
        when(mediaAssetRepository.findById("asset-1")).thenReturn(Optional.of(asset));

        // Act & Assert (Củ Cải 1 đòi xóa Asset của Củ Cải 2)
        assertThatThrownBy(() -> adminMovieManagementService.removeAsset("movie-1", "asset-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tài nguyên không thuộc về bộ phim này!");

        verify(mediaAssetRepository, never()).delete(any());
    }

    // --- Idempotent behavior tests (Task 14) ---

    @Test
    void whenHideAlreadyHiddenMovie_thenStatusRemainsHidden() {
        // Arrange: movie đã ở trạng thái HIDDEN
        Movie movie = createMockMovie();
        movie.setStatus(ContentStatus.HIDDEN);
        when(movieRepository.findById("movie-1")).thenReturn(Optional.of(movie));

        // Act: gọi hide lần nữa vẫn phải hoạt động bình thường không ném exception
        adminMovieManagementService.hideMovie("movie-1");

        // Assert: status vẫn HIDDEN, save được gọi (consistent, idempotent)
        assertThat(movie.getStatus()).isEqualTo(ContentStatus.HIDDEN);
        verify(movieRepository, times(1)).save(movie);
    }

    @Test
    void whenRestoreAlreadyActiveMovie_thenStatusRemainsActive() {
        // Arrange: movie đã ở trạng thái ACTIVE
        Movie movie = createMockMovie();
        movie.setStatus(ContentStatus.ACTIVE);
        when(movieRepository.findById("movie-1")).thenReturn(Optional.of(movie));

        // Act: gọi restore lần nữa vẫn phải hoạt động bình thường không ném exception
        adminMovieManagementService.restoreMovie("movie-1");

        // Assert: status vẫn ACTIVE, save được gọi (consistent, idempotent)
        assertThat(movie.getStatus()).isEqualTo(ContentStatus.ACTIVE);
        verify(movieRepository, times(1)).save(movie);
    }

    @Test
    void whenRestoreMovieNotFound_ThrowsException() {
        // Arrange
        when(movieRepository.findById("invalid-id")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> adminMovieManagementService.restoreMovie("invalid-id"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không tìm thấy phim lẻ");

        verify(movieRepository, never()).save(any());
    }

    // --- filterMovies logic tests (Bug A fix) ---

    @Test
    void filterMovies_WithNullStatusAndBlankKeyword_ReturnsAllMovies() {
        // Arrange: setup expected list
        List<Movie> allMovies = List.of(createMockMovie(), createMockMovie());
        when(movieRepository.findAllByOrderByTitleAsc()).thenReturn(allMovies);

        // Act
        List<Movie> result = adminMovieManagementService.filterMovies(null, null);

        // Assert: getAllMovies() path is taken, not findAllByStatusOrderByTitleAsc(null)
        assertThat(result).hasSize(2);
        verify(movieRepository, times(1)).findAllByOrderByTitleAsc();
        verify(movieRepository, never()).findAllByStatusOrderByTitleAsc(any());
    }

    @Test
    void filterMovies_WithNullStatusAndEmptyKeyword_ReturnsAllMovies() {
        // Blank string (chứa khoảng trắng) cũng phải trả về all movies
        List<Movie> allMovies = List.of(createMockMovie());
        when(movieRepository.findAllByOrderByTitleAsc()).thenReturn(allMovies);

        List<Movie> result = adminMovieManagementService.filterMovies(null, "   ");

        assertThat(result).hasSize(1);
        verify(movieRepository, times(1)).findAllByOrderByTitleAsc();
        verify(movieRepository, never()).findAllByStatusOrderByTitleAsc(any());
    }

    @Test
    void filterMovies_WithStatusOnly_ReturnsFilteredByStatus() {
        // Arrange
        List<Movie> activeMovies = List.of(createMockMovie());
        when(movieRepository.findAllByStatusOrderByTitleAsc(ContentStatus.ACTIVE)).thenReturn(activeMovies);

        // Act
        List<Movie> result = adminMovieManagementService.filterMovies(ContentStatus.ACTIVE, null);

        // Assert: getMoviesByStatus() path is taken
        assertThat(result).hasSize(1);
        verify(movieRepository, times(1)).findAllByStatusOrderByTitleAsc(ContentStatus.ACTIVE);
        verify(movieRepository, never()).findAllByOrderByTitleAsc();
    }
}
