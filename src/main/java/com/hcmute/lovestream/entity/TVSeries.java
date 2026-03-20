package com.hcmute.lovestream.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class TVSeries extends VideoContent {

    @OneToMany(mappedBy = "tvSeries", cascade = CascadeType.ALL)
    private List<Season> seasons;

    @Override
    public void getDetails() {
        // Cài đặt logic riêng cho TV Series
    }
}
