package com.maritel.trustay.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TBL_POST_LIKE",
        uniqueConstraints = @UniqueConstraint(name = "uk_post_like_post_member",
                columnNames = {"post_id", "member_id"}),
        indexes = {
                @Index(name = "idx_post_like_member", columnList = "member_id"),
                @Index(name = "idx_post_like_post", columnList = "post_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostLike extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_like_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Builder
    public PostLike(Post post, Member member) {
        this.post = post;
        this.member = member;
    }
}
