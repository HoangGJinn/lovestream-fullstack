package com.hcmute.lovestream.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true)
    private String phone;

    @Column(nullable = false)
    private String password;

    // Phân quyền: USER, VIP, ADMIN, CONTENT_MANAGER
    private String role;

    // Trạng thái kích hoạt tài khoản (false khi mới đăng ký)
    @Builder.Default
    private boolean isActive = false;
}