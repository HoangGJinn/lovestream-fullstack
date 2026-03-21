package com.hcmute.lovestream.entity;

import jakarta.persistence.*;
import lombok.*;


import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor

public class Genre {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    private String name;

    // Mối quan hệ nhiều-nhiều với VideoContent
    @ManyToMany(mappedBy = "genres")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<VideoContent> videoContents;
}
