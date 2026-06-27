package com.hcmute.lovestream.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class TVSeries extends VideoContent {

    /** Thời lượng trung bình mỗi tập (phút) */
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @OneToMany(mappedBy = "tvSeries", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Season> seasons = new ArrayList<>();

    @Override
    @Transient
    public int getTotalDurationMinutes() {
        if (seasons == null) return 0;
        return seasons.stream()
                .mapToInt(Season::getTotalDurationMinutes)
                .sum();
    }

    @Override
    public void getDetails() {
        System.out.println("TV Series: " + getTitle() + " | Total Duration: " + getTotalDurationMinutes() + " mins");
        if (seasons != null) {
            seasons.forEach(Season::getDetails);
        }
    }
}
