package com.hcmute.lovestream.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;
@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Movie extends VideoContent{
    private int durationMinutes; // Thời lượng

    @Temporal(TemporalType.DATE)
    private Date releaseDate;

    @Override
    public void getDetails() {
        // Cài đặt logic riêng cho Movie
    }
}
