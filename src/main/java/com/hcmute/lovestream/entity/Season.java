package com.hcmute.lovestream.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Season {
    @Id
    private String id;

    private int seasonNumber;
    private String name;
    private int releaseYear;

    @ManyToOne
    @JoinColumn(name = "tv_series_id")
    private TVSeries tvSeries;

    @OneToMany(mappedBy = "season", cascade = CascadeType.ALL)
    private List<Episode> episodes;
}
