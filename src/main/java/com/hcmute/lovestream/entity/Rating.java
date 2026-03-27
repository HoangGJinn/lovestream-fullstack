package com.hcmute.lovestream.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ratings", uniqueConstraints = {
        // Đảm bảo 1 user chỉ được đánh giá 1 phim tối đa 1 lần
        @UniqueConstraint(columnNames = {"user_id", "video_content_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Rating {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private int score; // Thang điểm (ví dụ: 1 đến 5 sao)

    @Column(columnDefinition = "TEXT")
    private String review; // Nhận xét về phim (tuỳ chọn)

    @Column(name = "like_count", nullable = false)
    private int likeCount = 0;
    @Column(name = "dislike_count", nullable = false)
    private int dislikeCount = 0;

    // Người đánh giá
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    // Phim được đánh giá
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_content_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private VideoContent videoContent;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 1 Đánh giá sẽ gắn liền với 1 Bình luận gốc
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id") // Tạo cột comment_id trong bảng ratings
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Comment comment;

}