package com.maritel.trustay.entity;

import com.maritel.trustay.constant.DeviceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "TBL_FCM_TOKEN", uniqueConstraints = {
        @UniqueConstraint(name = "uk_fcm_token_token", columnNames = "token")
}, indexes = {
        @Index(name = "idx_fcm_token_member", columnList = "member_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FcmToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fcm_token_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false, length = 500)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 20)
    private DeviceType deviceType;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @Builder
    public FcmToken(Member member, String token, DeviceType deviceType) {
        this.member = member;
        this.token = token;
        this.deviceType = deviceType;
        this.lastSeenAt = LocalDateTime.now();
    }

    public void touch() {
        this.lastSeenAt = LocalDateTime.now();
    }

    public void rebind(Member member, DeviceType deviceType) {
        this.member = member;
        this.deviceType = deviceType;
        this.lastSeenAt = LocalDateTime.now();
    }
}
