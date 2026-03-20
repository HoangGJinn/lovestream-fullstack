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

public class Genre {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    private String name;

    // Mối quan hệ nhiều-nhiều với VideoContent
    @ManyToMany(mappedBy = "genres")
    private List<VideoContent> videoContents;
}
