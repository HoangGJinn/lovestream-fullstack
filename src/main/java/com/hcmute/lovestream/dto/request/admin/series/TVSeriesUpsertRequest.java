package com.hcmute.lovestream.dto.request.admin.series;

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

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TVSeriesUpsertRequest {
    private String id;

    @NotBlank(message = "Ti\u00eau \u0111\u1ec1 kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1ed1ng")
    private String title;

    @NotBlank(message = "M\u00f4 t\u1ea3 kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1ed1ng")
    private String description;

    @NotNull(message = "N\u0103m ph\u00e1t h\u00e0nh kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1ed1ng")
    @Min(value = 1800, message = "N\u0103m ph\u00e1t h\u00e0nh kh\u00f4ng h\u1ee3p l\u1ec7")
    private Integer releaseYear;

    @NotNull(message = "Ph\u00e2n lo\u1ea1i \u0111\u1ed9 tu\u1ed5i kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1ed1ng")
    private AgeRating ageRating;

    @NotNull(message = "Ch\u1ea5t l\u01b0\u1ee3ng kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1ed1ng")
    private Quality quality;

    @NotNull(message = "Tr\u1ea1ng th\u00e1i kh\u00f4ng \u0111\u01b0\u1ee3c \u0111\u1ec3 tr\u1ed1ng")
    private ContentStatus status;

    @NotEmpty(message = "Vui l\u00f2ng ch\u1ecdn \u00edt nh\u1ea5t m\u1ed9t th\u1ec3 lo\u1ea1i")
    private List<String> genreIds;

    @Min(value = 1, message = "Th\u1eddi l\u01b0\u1ee3ng m\u1ed7i t\u1eadp ph\u1ea3i l\u1edbn h\u01a1n 0")
    private Integer durationMinutes;

    /** Chu\u1ed7i nhi\u1ec1u t\u00ean \u0111\u1ea1o di\u1ec5n, ph\u00e2n c\u00e1ch b\u1eb1ng d\u1ea5u ph\u1ea9y */
    private String directorNames;

    /** Chu\u1ed7i nhi\u1ec1u t\u00ean di\u1ec5n vi\u00ean, ph\u00e2n c\u00e1ch b\u1eb1ng d\u1ea5u ph\u1ea9y */
    private String castNames;
}
