package com.hcmute.lovestream.dto.request.contentmanager.movie;

import com.hcmute.lovestream.entity.enums.AgeRating;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.entity.enums.Quality;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieUpsertRequest {

    // Nếu là thao tác tạo mới thì trường này có thể rỗng, 
    // nếu là thao tác cập nhật (Update) thì Thymeleaf sẽ map ID này vào thẻ input hidden
    private String id;

    @NotBlank(message = "Tên phim không được để trống")
    private String title;

    @NotBlank(message = "Mô tả không được để trống")
    private String description;

    @NotNull(message = "Năm phát hành không được để trống")
    @Min(value = 1800, message = "Năm phát hành không hợp lệ (Phải lớn hơn hoặc bằng 1800)")
    private Integer releaseYear;

    @NotNull(message = "Ngày phát hành không được để trống")
    @DateTimeFormat(pattern = "yyyy-MM-dd") // Format date để Spring/Thymeleaf bind đúng type LocalDate html5
    private LocalDate releaseDate;

    @NotNull(message = "Thời lượng phim không được để trống")
    @Min(value = 1, message = "Thời lượng phim phải lớn hơn 0")
    private Integer durationMinutes;

    @NotNull(message = "Phân loại độ tuổi (Age Rating) không được để trống")
    private AgeRating ageRating;

    @NotNull(message = "Chất lượng phim (Quality) không được để trống")
    private Quality quality;

    @NotNull(message = "Trạng thái hiển thị (Status) không được để trống")
    private ContentStatus status;

    @NotEmpty(message = "Vui lòng chọn ít nhất một thể loại cho bộ phim")
    private List<String> genreIds;

    private String country;

    /** Chuỗi nhiều tên đạo diễn, phân tách bằng dấu phẩy */
    private String directorNames;

    /** Chuỗi nhiều tên diễn viên, phân tách bằng dấu phẩy */
    private String castNames;
}
