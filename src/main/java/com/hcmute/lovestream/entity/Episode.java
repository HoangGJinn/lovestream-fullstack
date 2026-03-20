package com.hcmute.lovestream.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Episode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    private List<MediaAsset> mediaAssets;
}
