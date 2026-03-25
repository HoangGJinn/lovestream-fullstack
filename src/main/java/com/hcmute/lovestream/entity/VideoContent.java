package com.hcmute.lovestream.entity;

import com.hcmute.lovestream.entity.enums.AgeRating;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.entity.enums.Quality;
import com.hcmute.lovestream.util.VietnameseNormalizer;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "video_content")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED) // Kế thừa JOINED

public abstract class VideoContent {
    // SỬA Ở ĐÂY: Đổi IDENTITY thành UUID cho kiểu String
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String title;

    @Column(name = "title_unsigned")
    private String titleUnsigned;

    /**
     * Slug SEO-friendly: ví dụ "Avengers: Endgame" → "avengers-endgame"
     * Được sinh tự động từ title khi tạo/cập nhật.
     * Unique để làm URL path.
     */
    @Column(unique = true)
    private String slug;

    @Column(columnDefinition = "TEXT")
    private String description;

    private int releaseYear;

    private String country;

    @Enumerated(EnumType.STRING)
    private AgeRating ageRating;

    @Enumerated(EnumType.STRING)
    private Quality quality;

    @Enumerated(EnumType.STRING)
    private ContentStatus status;

    private Double averageRating = 0.0;

    private int totalRatings = 0;

    // N-N Relationship với Genre
    // Đổi từ List sang Set
    @ManyToMany
    @JoinTable(
            name = "video_content_genre",
            joinColumns = @JoinColumn(name = "video_content_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Genre> genres = new HashSet<>();

    // 1-N với ContentCredit
    @OneToMany(mappedBy = "videoContent", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<ContentCredit> contentCredits;

    // 1-N với MediaAsset
    @OneToMany(mappedBy = "videoContent", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @BatchSize(size = 100)
    private List<MediaAsset> mediaAssets;

    // THÊM MỚI: 1-N với Comment (Một phim có nhiều bình luận)
    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Comment> comments = new ArrayList<>();
    // Các đánh giá dành cho phim này
    @OneToMany(mappedBy = "videoContent", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Rating> ratings = new ArrayList<>();

    // Các phòng đang chiếu phim này
    @OneToMany(mappedBy = "videoContent", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Room> rooms = new ArrayList<>();

    // Những lượt yêu thích dành cho phim này
    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<FavoriteList> favoritedByUsers = new ArrayList<>();

    // Lịch sử xem liên quan đến nội dung này
    @OneToMany(mappedBy = "videoContent", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<WatchHistory> watchHistories = new ArrayList<>();

    // Hàm này không lưu xuống Database, chỉ dùng để hỗ trợ Thymeleaf lấy ảnh Poster ra giao diện
    @Transient
    public String getPosterUrl() {
        if (this.mediaAssets != null) {
            for (MediaAsset asset : this.mediaAssets) {
                if (asset.getAssetType() == com.hcmute.lovestream.entity.enums.AssetType.POSTER) {
                    return asset.getAssetUrl();
                }
            }
        }
        return "https://via.placeholder.com/300x450?text=No+Poster"; // Ảnh mặc định nếu phim chưa có poster
    }

    /**
     * Trả về slug nếu có, fallback về id khi slug chưa được backfill.
     * Dùng trong Thymeleaf: ${movie.slugOrId}
     */
    @Transient
    public String getSlugOrId() {
        return (slug != null && !slug.isBlank()) ? slug : id;
    }

    @PrePersist
    @PreUpdate
    protected void syncSearchFields() {
        this.titleUnsigned = VietnameseNormalizer.normalize(this.title);
        // Sinh slug từ titleUnsigned: thay khoảng trắng bằng dấu gạch ngang
        if (this.title != null && !this.title.isBlank()) {
            this.slug = generateSlug(this.title);
        }
    }

    /**
     * Chuyển title thành slug SEO-friendly.
     * "Avengers: Endgame" → "avengers-endgame"
     * "Phim Mai" → "phim-mai"
     */
    public static String generateSlug(String title) {
        if (title == null) return "";
        String normalized = VietnameseNormalizer.normalize(title);
        // Xóa ký tự đặc biệt (giữ lại chữ cái, số, khoảng trắng)
        String cleaned = normalized.replaceAll("[^a-z0-9\\s]", "");
        // Thay nhiều khoảng trắng liên tiếp bằng 1 dấu gạch ngang
        return cleaned.trim().replaceAll("\\s+", "-");
    }

    public abstract void getDetails(); // Khai báo method như trong UML

}
