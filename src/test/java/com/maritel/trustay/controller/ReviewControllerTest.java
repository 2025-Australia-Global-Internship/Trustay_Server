package com.maritel.trustay.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maritel.trustay.dto.req.ReviewReq;
import com.maritel.trustay.dto.req.ReviewUpdateReq;
import com.maritel.trustay.dto.res.ReviewRes;
import com.maritel.trustay.service.ReviewService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
@AutoConfigureMockMvc(addFilters = false)
class ReviewControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    ReviewService reviewService;

    @MockBean
    com.maritel.trustay.config.CustomUserDetailsService customUserDetailsService;

    // ─────────────────────────────────────────────────────────────────────
    // 작성
    // ─────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("POST /api/trustay/reviews - 리뷰 작성 성공")
    void createReview_success() throws Exception {
        ReviewReq req = new ReviewReq();
        req.setHouseId(1L);
        req.setRating(5);
        req.setContent("좋아요");

        when(reviewService.createReview(eq("user@test.com"), any()))
                .thenReturn(ReviewRes.builder()
                        .id(10L)
                        .houseId(1L)
                        .rating(5)
                        .content("좋아요")
                        .build());

        mockMvc.perform(post("/api/trustay/reviews")
                        .principal((Principal) () -> "user@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.rating").value(5));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("POST /api/trustay/reviews - 거주이력 없으면 4000")
    void createReview_notEligible() throws Exception {
        ReviewReq req = new ReviewReq();
        req.setHouseId(1L);
        req.setRating(5);
        req.setContent("좋아요");

        when(reviewService.createReview(eq("user@test.com"), any()))
                .thenThrow(new IllegalStateException("거주 이력이 없습니다."));

        mockMvc.perform(post("/api/trustay/reviews")
                        .principal((Principal) () -> "user@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4000));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("POST /api/trustay/reviews - 잘못된 인자면 4000")
    void createReview_invalidArg() throws Exception {
        ReviewReq req = new ReviewReq();
        req.setHouseId(1L);
        req.setRating(5);
        req.setContent("hi");

        when(reviewService.createReview(eq("user@test.com"), any()))
                .thenThrow(new IllegalArgumentException("매물이 존재하지 않습니다."));

        mockMvc.perform(post("/api/trustay/reviews")
                        .principal((Principal) () -> "user@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4000));
    }

    // ─────────────────────────────────────────────────────────────────────
    // 수정
    // ─────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("PUT /api/trustay/reviews/10 - 리뷰 수정 성공")
    void updateReview_success() throws Exception {
        ReviewUpdateReq req = new ReviewUpdateReq();
        req.setRating(4);
        req.setContent("수정된 내용");

        when(reviewService.updateReview(eq("user@test.com"), eq(10L), any()))
                .thenReturn(ReviewRes.builder().id(10L).rating(4).content("수정된 내용").build());

        mockMvc.perform(put("/api/trustay/reviews/10")
                        .principal((Principal) () -> "user@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rating").value(4));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("PUT /api/trustay/reviews/99 - 없는 리뷰면 NOT_FOUND_REVIEW(4041)")
    void updateReview_notFound() throws Exception {
        ReviewUpdateReq req = new ReviewUpdateReq();
        req.setRating(4);

        when(reviewService.updateReview(eq("user@test.com"), eq(99L), any()))
                .thenThrow(new EntityNotFoundException("not found"));

        mockMvc.perform(put("/api/trustay/reviews/99")
                        .principal((Principal) () -> "user@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4041));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("PUT /api/trustay/reviews/10 - 본인 글 아니면 4030")
    void updateReview_forbidden() throws Exception {
        ReviewUpdateReq req = new ReviewUpdateReq();
        req.setRating(4);

        when(reviewService.updateReview(eq("user@test.com"), eq(10L), any()))
                .thenThrow(new IllegalStateException("본인이 작성한 리뷰만 수정할 수 있습니다."));

        mockMvc.perform(put("/api/trustay/reviews/10")
                        .principal((Principal) () -> "user@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4030));
    }

    // ─────────────────────────────────────────────────────────────────────
    // 삭제
    // ─────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("DELETE /api/trustay/reviews/10 - 리뷰 삭제 성공")
    void deleteReview_success() throws Exception {
        mockMvc.perform(delete("/api/trustay/reviews/10")
                        .principal((Principal) () -> "user@test.com"))
                .andExpect(status().isOk());
        verify(reviewService).deleteReview("user@test.com", 10L);
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("DELETE /api/trustay/reviews/99 - 없는 리뷰면 4041")
    void deleteReview_notFound() throws Exception {
        doThrow(new EntityNotFoundException("not found"))
                .when(reviewService).deleteReview("user@test.com", 99L);

        mockMvc.perform(delete("/api/trustay/reviews/99")
                        .principal((Principal) () -> "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4041));
    }

    // ─────────────────────────────────────────────────────────────────────
    // 조회
    // ─────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/trustay/reviews/house/1 - 매물 리뷰 목록")
    void getHouseReviews_success() throws Exception {
        when(reviewService.getHouseReviews(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        ReviewRes.builder().id(1L).rating(5).build(),
                        ReviewRes.builder().id(2L).rating(4).build()
                )));

        mockMvc.perform(get("/api/trustay/reviews/house/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/trustay/reviews/house/1/summary - 평점 요약")
    void getHouseRatingSummary_success() throws Exception {
        when(reviewService.getHouseRatingSummary(1L))
                .thenReturn(new ReviewService.RatingSummary(1L, 4.5, 10L));

        mockMvc.perform(get("/api/trustay/reviews/house/1/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.averageRating").value(4.5))
                .andExpect(jsonPath("$.data.reviewCount").value(10));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("GET /api/trustay/reviews/me - 내가 작성한 리뷰 목록")
    void getMyReviews_success() throws Exception {
        when(reviewService.getMyReviews(eq("user@test.com"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        ReviewRes.builder().id(1L).rating(5).build()
                )));

        mockMvc.perform(get("/api/trustay/reviews/me")
                        .principal((Principal) () -> "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1));
    }
}
