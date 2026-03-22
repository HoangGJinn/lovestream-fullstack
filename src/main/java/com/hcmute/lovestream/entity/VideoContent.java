package com.hcmute.lovestream.entity;

import com.hcmute.lovestream.entity.enums.AgeRating;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.entity.enums.Quality;
import jakarta.persistence.*;
import lombok.*;

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

    @Column(columnDefinition = "TEXT")
    private String description;

    private int releaseYear;

    @Enumerated(EnumType.STRING)
    private AgeRating ageRating;

    @Enumerated(EnumType.STRING)
    private Quality quality;

    @Enumerated(EnumType.STRING)
    private ContentStatus status;

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

    public abstract void getDetails(); // Khai báo method như trong UML

}
