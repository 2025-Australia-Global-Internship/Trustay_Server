package com.maritel.trustay.dto.res;

import com.maritel.trustay.entity.SharehouseRecentSearch;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RecentSearchRes {
    private Long id;
    private String keyword;
    private LocalDateTime searchedAt;

    public static RecentSearchRes from(SharehouseRecentSearch entity) {
        return RecentSearchRes.builder()
                .id(entity.getId())
                .keyword(entity.getKeyword())
                .searchedAt(entity.getSearchedAt())
                .build();
    }
}
