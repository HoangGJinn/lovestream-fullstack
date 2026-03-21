package com.hcmute.lovestream.entity;

import com.hcmute.lovestream.entity.enums.CreditType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentCredit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    private CreditType creditType;

    private String characterName; // Tên nhân vật (nếu là diễn viên)

    @ManyToOne
    @JoinColumn(name = "person_id")
    private Person person;

    @ManyToOne
    @JoinColumn(name = "video_content_id")
    private VideoContent videoContent;
}
