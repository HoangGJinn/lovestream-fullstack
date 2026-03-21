package com.hcmute.lovestream.entity;

import com.hcmute.lovestream.entity.enums.AssetType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    private AssetType assetType;

    private String assetUrl;

    // Có thể thuộc về 1 VideoContent (Movie/TVSeries chung)
    @ManyToOne
    @JoinColumn(name = "video_content_id")
    private VideoContent videoContent;

    // Hoặc có thể thuộc về 1 Tập phim cụ thể
    @ManyToOne
    @JoinColumn(name = "episode_id")
    private Episode episode;
}
