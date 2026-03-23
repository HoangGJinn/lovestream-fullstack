package com.hcmute.lovestream.dto.admin.movie;

import com.hcmute.lovestream.dto.request.admin.movie.MovieUpsertRequest;
import com.hcmute.lovestream.entity.enums.AgeRating;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.entity.enums.Quality;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class MovieUpsertRequestTest {

    private static Validator validator;

    @BeforeAll
    public static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private MovieUpsertRequest createValidRequest() {
        return MovieUpsertRequest.builder()
                .title("Avenger Endgame")
                .description("Một bộ phim bom tấn nổi bật")
                .releaseYear(2019)
                .releaseDate(LocalDate.of(2019, 4, 26))
                .durationMinutes(181)
                .ageRating(AgeRating.PG_13)
                .quality(Quality.HD)
                .status(ContentStatus.ACTIVE)
                .genreIds(List.of("123-uuid", "456-uuid"))
                .build();
    }

    @Test
    void whenValidData_thenNoErrors() {
        MovieUpsertRequest request = createValidRequest();

        Set<ConstraintViolation<MovieUpsertRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void whenMissingTitle_thenContainsError() {
        MovieUpsertRequest request = createValidRequest();
        request.setTitle("   "); // Khoảng trắng sẽ tính là Blank

        Set<ConstraintViolation<MovieUpsertRequest>> violations = validator.validateProperty(request, "title");
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Tên phim không được để trống");
    }

    @Test
    void whenNegativeOrZeroDuration_thenContainsError() {
        MovieUpsertRequest request = createValidRequest();
        request.setDurationMinutes(0);

        Set<ConstraintViolation<MovieUpsertRequest>> violations = validator.validateProperty(request, "durationMinutes");
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Thời lượng phim phải lớn hơn 0");
    }

    @Test
    void whenInvalidReleaseYear_thenContainsError() {
        MovieUpsertRequest request = createValidRequest();
        request.setReleaseYear(100);

        Set<ConstraintViolation<MovieUpsertRequest>> violations = validator.validateProperty(request, "releaseYear");
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Năm phát hành không hợp lệ (Phải lớn hơn hoặc bằng 1800)");
    }

    @Test
    void whenEmptyGenre_thenContainsError() {
        MovieUpsertRequest request = createValidRequest();
        request.setGenreIds(Collections.emptyList());

        Set<ConstraintViolation<MovieUpsertRequest>> violations = validator.validateProperty(request, "genreIds");
        assertThat(violations).isNotEmpty();
        assertThat(violations.iterator().next().getMessage()).isEqualTo("Vui lòng chọn ít nhất một thể loại cho bộ phim");
    }

    @Test
    void whenMissingEnums_thenContainsError() {
        MovieUpsertRequest request = createValidRequest();
        request.setAgeRating(null);
        request.setStatus(null);
        request.setQuality(null);

        Set<ConstraintViolation<MovieUpsertRequest>> violations = validator.validate(request);
        assertThat(violations).hasSize(3); // Cả 3 field Enum rớt validation @NotNull
    }
}
