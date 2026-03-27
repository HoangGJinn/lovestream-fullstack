package com.hcmute.lovestream.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rating_votes", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "rating_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingVote {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rating_id", nullable = false)
    private Rating rating;
    @Column(name = "is_like", nullable = false)
    private boolean isLike;
}
