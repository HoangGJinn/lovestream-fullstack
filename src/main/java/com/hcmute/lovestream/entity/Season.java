package com.hcmute.lovestream.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Season {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private int seasonNumber;
    private String name;
    private int releaseYear;

    @ManyToOne
    @JoinColumn(name = "tv_series_id")
    private TVSeries tvSeries;

    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Episode> episodes;
}
