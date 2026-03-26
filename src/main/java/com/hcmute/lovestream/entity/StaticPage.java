package com.hcmute.lovestream.entity;

import com.hcmute.lovestream.entity.enums.WebStaticPageType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "web_static_pages")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaticPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "page_type", nullable = false, unique = true, length = 50)
    private WebStaticPageType pageType;

    @Column(name = "html_content", columnDefinition = "TEXT", nullable = false)
    private String htmlContent;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

