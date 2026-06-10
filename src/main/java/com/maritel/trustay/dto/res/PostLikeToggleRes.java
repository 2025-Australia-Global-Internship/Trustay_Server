package com.maritel.trustay.dto.res;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PostLikeToggleRes {
    private Long postId;
    private Boolean liked;
    private Integer likeCount;
}
