package com.maritel.trustay.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 회원이 최근에 조회한 쉐어하우스 기록.
 * (member_id, house_id) 조합으로 한 건만 유지하고, 다시 조회되면 viewedAt 만 갱신한다.
 */
@Entity
@Table(name = "TBL_SHAREHOUSE_RECENT_VIEW", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"member_id", "house_id"})
}, indexes = {
        @Index(name = "idx_recent_view_member_viewedat", columnList = "member_id, viewedAt")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SharehouseRecentView extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sharehouse_recent_view_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "house_id", nullable = false)
    private Sharehouse sharehouse;

    @Column(name = "viewedAt", nullable = false)
    private LocalDateTime viewedAt;

    @Builder
    public SharehouseRecentView(Member member, Sharehouse sharehouse) {
        this.member = member;
        this.sharehouse = sharehouse;
        this.viewedAt = LocalDateTime.now();
    }

    public void touch() {
        this.viewedAt = LocalDateTime.now();
    }
}
