package com.hcmute.lovestream.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    // 1. Khóa chính String -> UUID (Tránh lỗi Incorrect column specifier)
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // 2. Nội dung bình luận
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // 3. Thời gian tạo bình luận
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 4. Quan hệ n - 1 với User (Ai là người bình luận?)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    // 5. Quan hệ n - 1 với VideoContent (Bình luận này thuộc về Phim/TV Series nào?)
    // Có thể null vì nếu là phim bộ thì người dùng sẽ bình luận vào từng Tập (Episode)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_content_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private VideoContent video;

    // 6. Quan hệ n - 1 với Episode (Bình luận này thuộc về Tập phim nào?)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Episode episode;

    // Bình luận cha (Nếu đây là một phản hồi, nó sẽ trỏ tới bình luận gốc. Nếu là bình luận gốc thì để null)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Comment parentComment;

    // Danh sách các bình luận con (Các phản hồi của bình luận này)
    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Comment> replies = new ArrayList<>();
}