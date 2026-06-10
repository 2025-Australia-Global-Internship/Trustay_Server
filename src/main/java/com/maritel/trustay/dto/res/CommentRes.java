package com.maritel.trustay.dto.res;

import com.maritel.trustay.entity.Comment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentRes {
    private Long id;
    private Long postId;
    private Long authorId;
    private String authorName;
    private String authorProfileImageUrl;
    private String content;
    private Boolean isDeleted;
    private LocalDateTime regTime;
    private LocalDateTime modTime;

    public static CommentRes from(Comment c) {
        String profileImageUrl = null;
        if (c.getAuthor().getProfile() != null
                && c.getAuthor().getProfile().getProfileImage() != null) {
            profileImageUrl = c.getAuthor().getProfile().getProfileImage().getImageUrl();
        }
        return CommentRes.builder()
                .id(c.getId())
                .postId(c.getPost().getId())
                .authorId(c.getAuthor().getId())
                .authorName(c.getAuthor().getName())
                .authorProfileImageUrl(profileImageUrl)
                .content(c.getContent())
                .isDeleted(c.getIsDeleted())
                .regTime(c.getRegTime())
                .modTime(c.getModTime())
                .build();
    }
}
