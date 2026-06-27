package com.hcmute.lovestream.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Season implements PlayableContent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private int seasonNumber;
    private String name;
    private int releaseYear;

    @ManyToOne
    @JoinColumn(name = "tv_series_id")
    private TVSeries tvSeries;

    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Episode> episodes = new ArrayList<>();

    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<MediaAsset> mediaAssets = new ArrayList<>();

    @Override
    public String getTitle() {
        return this.name;
    }

    @Override
    @Transient
    public int getTotalDurationMinutes() {
        if (episodes == null) return 0;
        return episodes.stream()
                .mapToInt(Episode::getTotalDurationMinutes)
                .sum();
    }

    @Override
    public void getDetails() {
        System.out.println(" * Season: " + getTitle() + " | Total Duration: " + getTotalDurationMinutes() + " mins");
        if (episodes != null) {
            episodes.forEach(Episode::getDetails);
        }
    }
}
