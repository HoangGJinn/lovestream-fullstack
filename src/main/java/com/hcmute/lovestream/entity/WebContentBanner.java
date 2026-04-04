package com.hcmute.lovestream.entity;

import com.hcmute.lovestream.entity.enums.WebContentBannerTargetType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "web_content_banners")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebContentBanner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    /**
     * Relative path under app upload dir, served publicly from "/uploads/**".
     * Example: "banners/uuid.png"
     */
    @Column(name = "image_path", nullable = false)
    private String imagePath;

    /**
     * Legacy raw URL from the old banner implementation.
     */
    @Column(name = "navigation_link")
    private String navigationLink;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 30)
    private WebContentBannerTargetType targetType;

    @Column(name = "target_ref_id", length = 100)
    private String targetRefId;

    @Column(name = "external_url")
    private String externalUrl;

    /**
     * Order for rendering on home slider (ascending).
     */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "is_displayed", nullable = false)
    private Boolean isDisplayed;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private String resolvedNavigationLink;

    @Transient
    private String resolvedTargetLabel;

    @Transient
    private String resolvedDescription;
}
