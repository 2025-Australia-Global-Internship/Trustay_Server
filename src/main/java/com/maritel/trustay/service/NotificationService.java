package com.maritel.trustay.service;

import com.maritel.trustay.constant.NotificationType;
import com.maritel.trustay.dto.req.FcmTokenReq;
import com.maritel.trustay.dto.res.NotificationRes;
import com.maritel.trustay.entity.FcmToken;
import com.maritel.trustay.entity.Member;
import com.maritel.trustay.entity.Notification;
import com.maritel.trustay.repository.FcmTokenRepository;
import com.maritel.trustay.repository.MemberRepository;
import com.maritel.trustay.repository.NotificationRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final MemberRepository memberRepository;

    // ────────────────────────────────────────────────────────────────────────
    // 알림 생성 (다른 서비스에서 호출하는 진입점)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * 알림을 DB에 저장하고, 등록된 FCM 토큰이 있다면 push 전송을 시도한다.
     * 현재는 Firebase Admin SDK 미연동 상태이므로 push는 로깅만 한다.
     * recipient 가 null 이거나 자기 자신에게 보내는 경우 무시한다.
     */
    @Transactional
    public void notify(Member recipient, NotificationType type, String title, String body, String linkUrl) {
        if (recipient == null) return;
        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .body(body)
                .linkUrl(linkUrl)
                .build();
        notificationRepository.save(notification);

        sendPushBestEffort(recipient, title, body, linkUrl);
    }

    /** 자기 자신에게는 보내지 않도록 senderId 와 비교한다. */
    @Transactional
    public void notifyExcludingSender(Member recipient, Long senderId,
                                      NotificationType type, String title, String body, String linkUrl) {
        if (recipient == null) return;
        if (senderId != null && recipient.getId().equals(senderId)) return;
        notify(recipient, type, title, body, linkUrl);
    }

    private void sendPushBestEffort(Member recipient, String title, String body, String linkUrl) {
        try {
            List<FcmToken> tokens = fcmTokenRepository.findByMember_Id(recipient.getId());
            if (tokens.isEmpty()) return;
            // TODO: Firebase Admin SDK 연동 후 실제 메시지 전송으로 교체
            log.info("[FCM-STUB] push to memberId={} tokens={} title='{}' body='{}' link='{}'",
                    recipient.getId(), tokens.size(), title, body, linkUrl);
        } catch (Exception e) {
            log.warn("[FCM-STUB] push send failed: {}", e.getMessage());
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // 조회 / 읽음 처리 / 삭제
    // ────────────────────────────────────────────────────────────────────────

    public Page<NotificationRes> listMyNotifications(String email, Pageable pageable) {
        Member me = findMember(email);
        return notificationRepository.findByRecipient_IdOrderByRegTimeDesc(me.getId(), pageable)
                .map(NotificationRes::from);
    }

    public long countUnread(String email) {
        Member me = findMember(email);
        return notificationRepository.countByRecipient_IdAndIsReadFalse(me.getId());
    }

    @Transactional
    public void markAsRead(String email, Long notificationId) {
        Member me = findMember(email);
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found."));
        if (!n.getRecipient().getId().equals(me.getId())) {
            throw new IllegalStateException("You can only manage your own notifications.");
        }
        if (Boolean.FALSE.equals(n.getIsRead())) {
            n.markAsRead();
        }
    }

    @Transactional
    public int markAllRead(String email) {
        Member me = findMember(email);
        return notificationRepository.markAllReadByRecipient(me.getId());
    }

    @Transactional
    public void delete(String email, Long notificationId) {
        Member me = findMember(email);
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found."));
        if (!n.getRecipient().getId().equals(me.getId())) {
            throw new IllegalStateException("You can only manage your own notifications.");
        }
        notificationRepository.delete(n);
    }

    // ────────────────────────────────────────────────────────────────────────
    // FCM 토큰 등록 / 해제
    // ────────────────────────────────────────────────────────────────────────

    /** token 이 이미 존재하면 소유자/디바이스 정보를 갱신, 없으면 신규 저장 */
    @Transactional
    public void registerFcmToken(String email, FcmTokenReq req) {
        Member me = findMember(email);
        fcmTokenRepository.findByToken(req.getToken())
                .ifPresentOrElse(
                        existing -> existing.rebind(me, req.getDeviceType()),
                        () -> fcmTokenRepository.save(FcmToken.builder()
                                .member(me)
                                .token(req.getToken())
                                .deviceType(req.getDeviceType())
                                .build())
                );
    }

    @Transactional
    public void removeFcmToken(String email, String token) {
        Member me = findMember(email);
        fcmTokenRepository.findByToken(token).ifPresent(t -> {
            if (!t.getMember().getId().equals(me.getId())) {
                throw new IllegalStateException("You can only delete your own tokens.");
            }
            fcmTokenRepository.delete(t);
        });
    }

    private Member findMember(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
    }
}
