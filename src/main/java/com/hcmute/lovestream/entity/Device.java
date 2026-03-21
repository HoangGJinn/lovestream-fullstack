package com.hcmute.lovestream.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id; // Trong sơ đồ thiết kế là String, ta dùng UUID tự động sinh cho an toàn

    @Column(name = "device_name")
    private String deviceName;

    private String os;

    @Column(name = "fcm_token")
    private String fcmToken;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    // Thuộc tính này thêm vào để phục vụ cho hàm isActive() và revokeAccess() trong sơ đồ
    @Builder.Default
    @Column(name = "is_active")
    private boolean isActive = true;

    // Thiết lập mối quan hệ n - 1 với bảng users
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;
}