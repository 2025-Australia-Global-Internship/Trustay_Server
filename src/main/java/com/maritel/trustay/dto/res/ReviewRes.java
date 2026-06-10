package com.maritel.trustay.dto.res;

import com.maritel.trustay.entity.Review;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReviewRes {
    private Long id;
    private Long houseId;
    private Long authorId;
    private String authorName;
    private String authorProfileImageUrl;
    private Integer rating;
    private String content;
    private LocalDateTime regTime;
    private LocalDateTime modTime;

    public static ReviewRes from(Review review) {
        String profileImageUrl = null;
        if (review.getAuthor().getProfile() != null
                && review.getAuthor().getProfile().getProfileImage() != null) {
            profileImageUrl = review.getAuthor().getProfile().getProfileImage().getImageUrl();
        }
        return ReviewRes.builder()
                .id(review.getId())
                .houseId(review.getTargetHouse() != null ? review.getTargetHouse().getId() : null)
                .authorId(review.getAuthor().getId())
                .authorName(review.getAuthor().getName())
                .authorProfileImageUrl(profileImageUrl)
                .rating(review.getRating())
                .content(review.getContent())
                .regTime(review.getRegTime())
                .modTime(review.getModTime())
                .build();
    }
}
