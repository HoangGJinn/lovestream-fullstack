package com.hcmute.lovestream.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "favorite_lists", uniqueConstraints = {
        // Ràng buộc: Một user chỉ được thêm 1 phim vào danh sách yêu thích tối đa 1 lần
        @UniqueConstraint(columnNames = {"user_id", "video_content_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FavoriteList {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Thay Customer thành User như ý tưởng của bạn
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    // Phim được thêm vào danh sách
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_content_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private VideoContent video;

    // Thời gian phim được thêm vào danh sách (tương đương addedAt trong UML)
    @CreationTimestamp
    @Column(name = "added_at", updatable = false)
    private LocalDateTime addedAt;
}