package com.hcmute.lovestream.dto.request.webcontent;

import com.hcmute.lovestream.entity.enums.WebContentBannerTargetType;
import com.hcmute.lovestream.entity.enums.WebStaticPageType;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebContentBannerUpsertRequest {

    private Long id;

    @NotBlank(message = "Tiêu đề banner không được để trống.")
    private String title;

    @Builder.Default
    private WebContentBannerTargetType targetType = WebContentBannerTargetType.NONE;

    private String movieTargetId;

    private String seriesTargetId;

    private WebStaticPageType staticPageTarget;

    private String externalUrl;

    /**
     * Ascending order on home slider.
     */
    private Integer displayOrder;

    private Boolean isDisplayed;

    /**
     * Upload a banner image (local only). Required for create, optional for update.
     */
    private MultipartFile bannerImage;

    /**
     * Keep current image path when updating without a new file.
     */
    private String existingImagePath;
}
