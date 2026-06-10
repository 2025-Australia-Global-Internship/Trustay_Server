package com.maritel.trustay.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maritel.trustay.dto.res.NotificationRes;
import com.maritel.trustay.service.NotificationService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    NotificationService notificationService;

    @MockBean
    com.maritel.trustay.config.CustomUserDetailsService customUserDetailsService;

    // ─────────────────────────────────────────────────────────────────────
    // FCM 토큰
    // ─────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("POST /api/trustay/notifications/fcm-token - 등록 성공")
    void registerFcmToken_success() throws Exception {
        String body = "{\"token\":\"abcd\",\"deviceType\":\"ANDROID\"}";

        mockMvc.perform(post("/api/trustay/notifications/fcm-token")
                        .principal((Principal) () -> "user@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(notificationService).registerFcmToken(eq("user@test.com"), any());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("POST /api/trustay/notifications/fcm-token - 잘못된 인자면 4000")
    void registerFcmToken_invalidArg() throws Exception {
        String body = "{\"token\":\"abcd\",\"deviceType\":\"ANDROID\"}";

        doThrow(new IllegalArgumentException("이미 다른 사용자의 토큰입니다."))
                .when(notificationService).registerFcmToken(eq("user@test.com"), any());

        mockMvc.perform(post("/api/trustay/notifications/fcm-token")
                        .principal((Principal) () -> "user@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4000));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("DELETE /api/trustay/notifications/fcm-token?token=... - 토큰 제거")
    void removeFcmToken_success() throws Exception {
        mockMvc.perform(delete("/api/trustay/notifications/fcm-token")
                        .principal((Principal) () -> "user@test.com")
                        .param("token", "abcd"))
                .andExpect(status().isOk());
        verify(notificationService).removeFcmToken("user@test.com", "abcd");
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("DELETE /api/trustay/notifications/fcm-token - 본인 토큰 아니면 4030")
    void removeFcmToken_forbidden() throws Exception {
        doThrow(new IllegalStateException("본인 토큰만 삭제할 수 있습니다."))
                .when(notificationService).removeFcmToken("user@test.com", "abcd");

        mockMvc.perform(delete("/api/trustay/notifications/fcm-token")
                        .principal((Principal) () -> "user@test.com")
                        .param("token", "abcd"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4030));
    }

    // ─────────────────────────────────────────────────────────────────────
    // 알림 조회
    // ─────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("GET /api/trustay/notifications - 내 알림 목록")
    void listMyNotifications_success() throws Exception {
        when(notificationService.listMyNotifications(eq("user@test.com"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        NotificationRes.builder().id(1L).title("새 알림").isRead(false).build()
                )));

        mockMvc.perform(get("/api/trustay/notifications")
                        .principal((Principal) () -> "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("새 알림"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("GET /api/trustay/notifications/unread-count - 안읽은 개수")
    void unreadCount_success() throws Exception {
        when(notificationService.countUnread("user@test.com")).thenReturn(7L);

        mockMvc.perform(get("/api/trustay/notifications/unread-count")
                        .principal((Principal) () -> "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(7));
    }

    // ─────────────────────────────────────────────────────────────────────
    // 읽음 처리
    // ─────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("PATCH /api/trustay/notifications/1/read - 단일 읽음")
    void markRead_success() throws Exception {
        mockMvc.perform(patch("/api/trustay/notifications/1/read")
                        .principal((Principal) () -> "user@test.com"))
                .andExpect(status().isOk());
        verify(notificationService).markAsRead("user@test.com", 1L);
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("PATCH /api/trustay/notifications/99/read - 없으면 NOT_FOUND_NOTIFICATION(4045)")
    void markRead_notFound() throws Exception {
        doThrow(new EntityNotFoundException("not found"))
                .when(notificationService).markAsRead("user@test.com", 99L);

        mockMvc.perform(patch("/api/trustay/notifications/99/read")
                        .principal((Principal) () -> "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4045));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("POST /api/trustay/notifications/read-all - 전체 읽음")
    void markAllRead_success() throws Exception {
        when(notificationService.markAllRead("user@test.com")).thenReturn(5);

        mockMvc.perform(post("/api/trustay/notifications/read-all")
                        .principal((Principal) () -> "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.updated").value(5));
    }

    // ─────────────────────────────────────────────────────────────────────
    // 삭제
    // ─────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("DELETE /api/trustay/notifications/1 - 알림 삭제")
    void deleteNotification_success() throws Exception {
        mockMvc.perform(delete("/api/trustay/notifications/1")
                        .principal((Principal) () -> "user@test.com"))
                .andExpect(status().isOk());
        verify(notificationService).delete("user@test.com", 1L);
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("DELETE /api/trustay/notifications/99 - 없으면 NOT_FOUND_NOTIFICATION(4045)")
    void deleteNotification_notFound() throws Exception {
        doThrow(new EntityNotFoundException("not found"))
                .when(notificationService).delete("user@test.com", 99L);

        mockMvc.perform(delete("/api/trustay/notifications/99")
                        .principal((Principal) () -> "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4045));
    }
}
