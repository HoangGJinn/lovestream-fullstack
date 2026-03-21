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
}