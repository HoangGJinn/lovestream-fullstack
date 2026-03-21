package com.hcmute.lovestream.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "service_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServicePlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false)
    private BigDecimal price; // Dùng BigDecimal cho tiền tệ để tránh sai số thập phân

    @Column(nullable = false)
    private String resolution; // Chất lượng video (VD: 1080p, 4K)

    @Column(name = "max_screens", nullable = false)
    private int maxScreens; // Số thiết bị xem đồng thời

    @Column(name = "duration_days", nullable = false)
    private int durationDays; // Thời hạn gói (VD: 30 ngày)

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active")
    @Builder.Default
    private boolean isActive = true; // Ẩn/hiện gói cước

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<Subscription> subscriptions = new ArrayList<>();

    @OneToMany(mappedBy = "servicePlan", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();
}