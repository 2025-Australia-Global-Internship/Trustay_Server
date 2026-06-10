package com.maritel.trustay.controller;

import com.maritel.trustay.dto.req.FcmTokenReq;
import com.maritel.trustay.dto.res.DataResponse;
import com.maritel.trustay.dto.res.NotificationRes;
import com.maritel.trustay.dto.res.PageResponse;
import com.maritel.trustay.dto.res.ResponseCode;
import com.maritel.trustay.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/trustay/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notification API", description = "사용자 알림 및 FCM 디바이스 토큰 관리")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "FCM 디바이스 토큰 등록/갱신",
            description = "앱 실행 또는 로그인 시 호출. 동일 토큰이 이미 존재하면 소유자/디바이스 정보를 갱신합니다.")
    @PostMapping("/fcm-token")
    public ResponseEntity<DataResponse<Void>> registerFcmToken(
            Principal principal,
            @Valid @RequestBody FcmTokenReq req) {
        try {
            notificationService.registerFcmToken(principal.getName(), req);
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "FCM 디바이스 토큰 제거", description = "로그아웃 시 호출합니다.")
    @DeleteMapping("/fcm-token")
    public ResponseEntity<DataResponse<Void>> removeFcmToken(
            Principal principal,
            @RequestParam String token) {
        try {
            notificationService.removeFcmToken(principal.getName(), token);
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (IllegalStateException e) {
            return forbidden(e.getMessage());
        }
    }

    @Operation(summary = "내 알림 목록 (최신순, 페이징)")
    @GetMapping
    public ResponseEntity<DataResponse<PageResponse<NotificationRes>>> listMyNotifications(
            Principal principal,
            @PageableDefault(size = 20, sort = "regTime", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<NotificationRes> page = notificationService.listMyNotifications(principal.getName(), pageable);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, new PageResponse<>(page)));
    }

    @Operation(summary = "안 읽은 알림 개수")
    @GetMapping("/unread-count")
    public ResponseEntity<DataResponse<Map<String, Long>>> unreadCount(Principal principal) {
        long count = notificationService.countUnread(principal.getName());
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, Map.of("unreadCount", count)));
    }

    @Operation(summary = "단일 알림 읽음 처리")
    @PatchMapping("/{id}/read")
    public ResponseEntity<DataResponse<Void>> markRead(
            Principal principal,
            @PathVariable Long id) {
        try {
            notificationService.markAsRead(principal.getName(), id);
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.ok(DataResponse.of(ResponseCode.NOT_FOUND_NOTIFICATION));
        } catch (IllegalStateException e) {
            return forbidden(e.getMessage());
        }
    }

    @Operation(summary = "전체 알림 읽음 처리")
    @PostMapping("/read-all")
    public ResponseEntity<DataResponse<Map<String, Integer>>> markAllRead(Principal principal) {
        int updated = notificationService.markAllRead(principal.getName());
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, Map.of("updated", updated)));
    }

    @Operation(summary = "알림 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<DataResponse<Void>> deleteNotification(
            Principal principal,
            @PathVariable Long id) {
        try {
            notificationService.delete(principal.getName(), id);
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.ok(DataResponse.of(ResponseCode.NOT_FOUND_NOTIFICATION));
        } catch (IllegalStateException e) {
            return forbidden(e.getMessage());
        }
    }

    private static <T> ResponseEntity<DataResponse<T>> badRequest(String message) {
        return ResponseEntity.ok(DataResponse.of(ResponseCode.NOT_VALID.getCode(), message, null));
    }

    private static <T> ResponseEntity<DataResponse<T>> forbidden(String message) {
        return ResponseEntity.ok(DataResponse.of(ResponseCode.FORBIDDEN.getCode(), message, null));
    }
}
