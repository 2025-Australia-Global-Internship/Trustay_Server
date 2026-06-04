package com.maritel.trustay.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "TBL_IMAGE")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Image extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long id;

    @Column(nullable = false, length = 500)
    private String imageUrl; // S3나 서버 저장 경로

    @Column(length = 100)
    private String originalName; // 원본 파일명

    @Builder
    public Image(String imageUrl, String originalName) {
        this.imageUrl = imageUrl;
        this.originalName = originalName;
    }
    
    @PrePersist
    void ensureOriginalName() {
        if (originalName != null && !originalName.isBlank()) {
            return;
        }
        
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        
        String normalizedUrl = imageUrl.replace('\\', '/');
        int lastSlash = normalizedUrl.lastIndexOf('/');
        this.originalName = lastSlash >= 0 && lastSlash < normalizedUrl.length() - 1
                ? normalizedUrl.substring(lastSlash + 1)
                : normalizedUrl;
    }
}
