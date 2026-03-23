package com.hcmute.lovestream.repository;

import com.hcmute.lovestream.entity.Genre;
import com.hcmute.lovestream.entity.Movie;
import com.hcmute.lovestream.entity.VideoContent;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class VideoContentRepositoryTest {

    @Autowired
    private VideoContentRepository videoContentRepository;

    @Autowired
    private TestEntityManager entityManager; // Dùng EntityManager để insert các Entity phụ thuộc dễ dàng

    @BeforeEach
    void setUp() {
        // Tạo Genre chung
        Genre actionGenre = new Genre();
        actionGenre.setName("Action");
        entityManager.persist(actionGenre);

        // Phim 1 - HOẠT ĐỘNG
        Movie activeMovie = new Movie();
        activeMovie.setTitle("Active Action Movie");
        activeMovie.setStatus(ContentStatus.ACTIVE);
        activeMovie.getGenres().add(actionGenre);
        entityManager.persist(activeMovie);

        // Phim 2 - BỊ ẨN
        Movie hiddenMovie = new Movie();
        hiddenMovie.setTitle("Hidden Action Movie");
        hiddenMovie.setStatus(ContentStatus.HIDDEN);
        hiddenMovie.getGenres().add(actionGenre);
        entityManager.persist(hiddenMovie);

        entityManager.flush();
    }

    @Test
    void testFindDistinctByStatusAndGenres_Name() {
        // Query Status = ACTIVE và Genre = Action
        List<VideoContent> activeContents = videoContentRepository
                .findDistinctByStatusAndGenres_Name(ContentStatus.ACTIVE, "Action");

        // Chỉ "Active Action Movie" được hiển thị, bộ phim bị khóa sẽ biến mất khỏi kết quả
        assertThat(activeContents).hasSize(1);
        assertThat(activeContents.get(0).getTitle()).isEqualTo("Active Action Movie");

        // Phục vụ kiểm tra ngược: Query Status = HIDDEN
        List<VideoContent> hiddenContents = videoContentRepository
                .findDistinctByStatusAndGenres_Name(ContentStatus.HIDDEN, "Action");

        assertThat(hiddenContents).hasSize(1);
        assertThat(hiddenContents.get(0).getTitle()).isEqualTo("Hidden Action Movie");
    }

    @Test
    void testFindDistinctByStatus_EmptyGenre_ReturnsEmptyList() {
        // Genre "Comedy" không có phim nào → kết quả phải rỗng, không gây lỗi
        List<VideoContent> result = videoContentRepository
                .findDistinctByStatusAndGenres_Name(ContentStatus.ACTIVE, "Comedy");

        assertThat(result).isEmpty();
    }

    @Test
    void testFindDistinctByStatus_MultiGenreMovie_NoDuplicate() {
        // Tạo một phim ACTIVE thuộc cả "Action" và "Drama" — distinct phải đảm bảo chỉ trả về 1 kết quả
        Genre dramaGenre = new Genre();
        dramaGenre.setName("Drama");
        entityManager.persist(dramaGenre);

        Genre actionGenre2 = entityManager
                .getEntityManager()
                .createQuery("SELECT g FROM Genre g WHERE g.name = 'Action'", Genre.class)
                .getSingleResult();

        Movie multiGenreMovie = new Movie();
        multiGenreMovie.setTitle("Multi Genre Active Movie");
        multiGenreMovie.setStatus(ContentStatus.ACTIVE);
        multiGenreMovie.getGenres().add(actionGenre2);
        multiGenreMovie.getGenres().add(dramaGenre);
        entityManager.persist(multiGenreMovie);
        entityManager.flush();

        List<VideoContent> result = videoContentRepository
                .findDistinctByStatusAndGenres_Name(ContentStatus.ACTIVE, "Action");

        // "Multi Genre Active Movie" và "Active Action Movie" phải có đúng 2 kết quả không trùng lặp
        assertThat(result).hasSize(2);
        long distinctCount = result.stream().map(VideoContent::getId).distinct().count();
        assertThat(distinctCount).isEqualTo(result.size()); // Không có duplicate ID
    }
}
