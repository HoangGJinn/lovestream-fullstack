package com.hcmute.lovestream.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import jakarta.persistence.Transient;
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
    @Transient
    public int getTotalDurationMinutes() {
        return this.durationMinutes;
    }

    @Override
    public void getDetails() {
        System.out.println("Movie: " + getTitle() + " | Duration: " + getTotalDurationMinutes() + " mins");
    }
}
