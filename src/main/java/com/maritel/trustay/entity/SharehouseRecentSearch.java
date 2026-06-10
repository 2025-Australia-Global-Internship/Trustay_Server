package com.maritel.trustay.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 회원이 최근에 검색한 키워드 기록.
 * (member_id, keyword) 조합으로 1건만 유지하고, 동일 키워드를 다시 검색하면 searchedAt 만 갱신한다.
 */
@Entity
@Table(name = "TBL_SHAREHOUSE_RECENT_SEARCH", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"member_id", "keyword"})
}, indexes = {
        @Index(name = "idx_recent_search_member_searchedat", columnList = "member_id, searchedAt")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SharehouseRecentSearch extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sharehouse_recent_search_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "keyword", nullable = false, length = 100)
    private String keyword;

    @Column(name = "searchedAt", nullable = false)
    private LocalDateTime searchedAt;

    @Builder
    public SharehouseRecentSearch(Member member, String keyword) {
        this.member = member;
        this.keyword = keyword;
        this.searchedAt = LocalDateTime.now();
    }

    public void touch() {
        this.searchedAt = LocalDateTime.now();
    }
}
