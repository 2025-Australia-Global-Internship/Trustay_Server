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
@Tag(name = "Notification API", description = "User notifications and FCM device token management.")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Register or refresh an FCM device token.",
            description = "Call this on app launch or login. If the same token already exists, its owner/device info is updated.")
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

    @Operation(summary = "Remove an FCM device token.", description = "Call this on logout.")
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

    @Operation(summary = "List my notifications (most recent first, paginated).")
    @GetMapping
    public ResponseEntity<DataResponse<PageResponse<NotificationRes>>> listMyNotifications(
            Principal principal,
            @PageableDefault(size = 20, sort = "regTime", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<NotificationRes> page = notificationService.listMyNotifications(principal.getName(), pageable);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, new PageResponse<>(page)));
    }

    @Operation(summary = "Get unread notification count.")
    @GetMapping("/unread-count")
    public ResponseEntity<DataResponse<Map<String, Long>>> unreadCount(Principal principal) {
        long count = notificationService.countUnread(principal.getName());
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, Map.of("unreadCount", count)));
    }

    @Operation(summary = "Mark a notification as read.")
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

    @Operation(summary = "Mark all notifications as read.")
    @PostMapping("/read-all")
    public ResponseEntity<DataResponse<Map<String, Integer>>> markAllRead(Principal principal) {
        int updated = notificationService.markAllRead(principal.getName());
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, Map.of("updated", updated)));
    }

    @Operation(summary = "Delete a notification.")
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

    /** 어떤 응답 타입에도 안전하게 사용할 수 있는 ResponseCode 기반 에러 응답 헬퍼 */
    private static <T> ResponseEntity<DataResponse<T>> error(ResponseCode code) {
        return ResponseEntity.ok(DataResponse.of(code.getCode(), code.getMessage(), null));
    }
}
