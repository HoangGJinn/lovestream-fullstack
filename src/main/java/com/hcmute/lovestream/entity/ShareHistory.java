package com.hcmute.lovestream.entity;

import com.hcmute.lovestream.entity.enums.SharePlatform;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "share_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShareHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_content_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private VideoContent videoContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SharePlatform platform;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime sharedAt;
}
