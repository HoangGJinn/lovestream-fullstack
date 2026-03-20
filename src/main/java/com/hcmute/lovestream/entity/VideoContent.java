package com.hcmute.lovestream.entity;

import com.hcmute.lovestream.entity.enums.AgeRating;
import com.hcmute.lovestream.entity.enums.ContentStatus;
import com.hcmute.lovestream.entity.enums.Quality;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED) // Kế thừa JOINED

public abstract class VideoContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private int releaseYear;

    @Enumerated(EnumType.STRING)
    private AgeRating ageRating;

    @Enumerated(EnumType.STRING)
    private Quality quality;

    @Enumerated(EnumType.STRING)
    private ContentStatus status;

    // N-N Relationship với Genre
    @ManyToMany
    @JoinTable(
            name = "video_content_genre",
            joinColumns = @JoinColumn(name = "video_content_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id")
    )
    private List<Genre> genres;

    // 1-N với ContentCredit
    @OneToMany(mappedBy = "videoContent", cascade = CascadeType.ALL)
    private List<ContentCredit> contentCredits;

    // 1-N với MediaAsset
    @OneToMany(mappedBy = "videoContent", cascade = CascadeType.ALL)
    private List<MediaAsset> mediaAssets;

    public abstract void getDetails(); // Khai báo method như trong UML
}
