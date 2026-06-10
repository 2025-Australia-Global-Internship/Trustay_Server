package com.maritel.trustay.dto.res;

import com.maritel.trustay.constant.NotificationType;
import com.maritel.trustay.entity.Notification;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationRes {
    private Long id;
    private NotificationType type;
    private String title;
    private String body;
    private String linkUrl;
    private Boolean isRead;
    private LocalDateTime regTime;

    public static NotificationRes from(Notification n) {
        return NotificationRes.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .body(n.getBody())
                .linkUrl(n.getLinkUrl())
                .isRead(n.getIsRead())
                .regTime(n.getRegTime())
                .build();
    }
}
