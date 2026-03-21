package com.hcmute.lovestream.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Episode {
    // SỬA Ở ĐÂY: Đổi IDENTITY thành UUID
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private int episodeNumber;
    private String title;
    private int durationInMinutes;

    @Temporal(TemporalType.DATE)
    private Date airDate;

    @ManyToOne
    @JoinColumn(name = "season_id")
    private Season season;

    // Các tài nguyên Media riêng cho loại tập phim
    @OneToMany(mappedBy = "episode", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<MediaAsset> mediaAssets;

    // THÊM MỚI: 1-N với Comment (Một tập phim có nhiều bình luận)
    @OneToMany(mappedBy = "episode", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Comment> comments = new ArrayList<>();
}
