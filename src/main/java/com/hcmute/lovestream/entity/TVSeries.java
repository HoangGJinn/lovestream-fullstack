package com.hcmute.lovestream.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
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
    public void getDetails() {
        // Cài đặt logic riêng cho TV Series
    }
}
