package com.hcmute.lovestream.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "user_series_watch_state",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_series_watch_state_user_series", columnNames = {"user_id", "series_id"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSeriesWatchState {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "series_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private TVSeries series;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_watched_episode_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Episode lastWatchedEpisode;

    @Column(name = "last_watched_at", nullable = false)
    @Builder.Default
    private LocalDateTime lastWatchedAt = LocalDateTime.now();

    @Column(name = "notifications_enabled", nullable = false)
    @Builder.Default
    private boolean notificationsEnabled = true;
}
