package com.maritel.trustay.entity;

import com.maritel.trustay.constant.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "TBL_NOTIFICATION",
        indexes = {
                @Index(name = "idx_notification_recipient_reg", columnList = "recipient_id, regTime"),
                @Index(name = "idx_notification_recipient_unread", columnList = "recipient_id, is_read")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Member recipient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    // 클릭 시 이동할 in-app 라우트 (예: "/sharehouse/12", "/chat/room/3")
    @Column(name = "link_url", length = 500)
    private String linkUrl;

    @Column(name = "is_read", nullable = false)
    @ColumnDefault("false")
    private Boolean isRead = false;

    @Builder
    public Notification(Member recipient, NotificationType type, String title, String body, String linkUrl) {
        this.recipient = recipient;
        this.type = type;
        this.title = title;
        this.body = body;
        this.linkUrl = linkUrl;
        this.isRead = false;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
