package com.hcmute.lovestream.entity;

import com.hcmute.lovestream.entity.enums.TypeNotification;
import com.hcmute.lovestream.entity.enums.UserNotificationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {

    // CHÚ Ý: Dùng UUID thay vì IDENTITY cho cột String
    // Không gặp lỗi "Incorrect column specifier"!
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "sent_at", nullable = false)
    @Builder.Default
    private LocalDateTime sentAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeNotification type;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // Trạng thái thông báo (Mặc định khi nhận được là Chưa đọc)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserNotificationStatus status = UserNotificationStatus.UNREAD;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // Khóa ngoại trong CSDL
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;
}