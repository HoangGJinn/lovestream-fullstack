package com.hcmute.lovestream.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "watch_history",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_watch_history_user_video", columnNames = {"user_id", "video_content_id"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WatchHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "video_content_id", nullable = false)
    private VideoContent videoContent;

    @Column(nullable = false)
    private Double progressSeconds = 0.0;

    @Column(nullable = false)
    private Double durationSeconds = 0.0;

    @Column(nullable = false)
    private LocalDateTime lastWatchedAt = LocalDateTime.now();

    @Column(nullable = false)
    private boolean completed;
}

