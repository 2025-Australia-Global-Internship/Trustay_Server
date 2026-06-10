package com.maritel.trustay.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maritel.trustay.dto.req.PostReq;
import com.maritel.trustay.dto.req.PostUpdateReq;
import com.maritel.trustay.dto.res.CommentRes;
import com.maritel.trustay.dto.res.PostLikeToggleRes;
import com.maritel.trustay.dto.res.PostRes;
import com.maritel.trustay.service.CommentService;
import com.maritel.trustay.service.PostService;
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
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostController.class)
@AutoConfigureMockMvc(addFilters = false)
class PostControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    PostService postService;

    @MockBean
    CommentService commentService;

    @MockBean
    com.maritel.trustay.config.CustomUserDetailsService customUserDetailsService;

    // ─────────────────────────────────────────────────────────────────────
    // 게시글 CRUD
    // ─────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("POST /api/trustay/posts - 게시글 작성")
    void createPost_success() throws Exception {
        PostReq req = new PostReq();
        req.setCommunityId(1L);
        req.setTitle("제목");
        req.setContent("내용");

        PostRes res = PostRes.builder().id(1L).title("제목").build();
        when(postService.createPost(eq("user@test.com"), any())).thenReturn(res);

        mockMvc.perform(post("/api/trustay/posts")
                        .principal((Principal) () -> "user@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("제목"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("GET /api/trustay/posts/1 - 게시글 상세")
    void getPostDetail_success() throws Exception {
        PostRes res = PostRes.builder().id(1L).title("제목").viewCount(1).build();
        when(postService.getPostDetail(eq(1L), nullable(String.class))).thenReturn(res);

        mockMvc.perform(get("/api/trustay/posts/1")
                        .principal((Principal) () -> "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("GET /api/trustay/posts/me - 내가 작성한 게시글")
    void getMyPosts_success() throws Exception {
        when(postService.getMyPosts(eq("user@test.com"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        PostRes.builder().id(1L).title("내 글").build()
                )));

        mockMvc.perform(get("/api/trustay/posts/me")
                        .principal((Principal) () -> "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("내 글"));
    }

    @Test
    @DisplayName("GET /api/trustay/posts/community/1 - 커뮤니티 게시글 목록")
    void getCommunityPosts_success() throws Exception {
        when(postService.getCommunityPosts(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/trustay/posts/community/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("GET /api/trustay/posts/sharehouse/1 - 쉐어하우스 커뮤니티 게시글 목록")
    void getSharehouseCommunityPosts_success() throws Exception {
        when(postService.getSharehouseCommunityPosts(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/trustay/posts/sharehouse/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("GET /api/trustay/posts/feed - 전체 피드")
    void getAllPosts_success() throws Exception {
        when(postService.getAllPosts(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/trustay/posts/feed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("PUT /api/trustay/posts/1 - 게시글 수정")
    void updatePost_success() throws Exception {
        PostUpdateReq req = new PostUpdateReq();
        req.setTitle("수정 제목");
        req.setContent("수정 내용");

        mockMvc.perform(put("/api/trustay/posts/1")
                        .principal((Principal) () -> "user@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
        verify(postService).updatePost(eq("user@test.com"), eq(1L), any());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("DELETE /api/trustay/posts/1 - 게시글 삭제")
    void deletePost_success() throws Exception {
        mockMvc.perform(delete("/api/trustay/posts/1")
                        .principal((Principal) () -> "user@test.com"))
                .andExpect(status().isOk());
        verify(postService).deletePost("user@test.com", 1L);
    }

    // ─────────────────────────────────────────────────────────────────────
    // 좋아요 토글
    // ─────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("POST /api/trustay/posts/1/like - 좋아요 토글")
    void toggleLike_success() throws Exception {
        when(postService.toggleLike("user@test.com", 1L))
                .thenReturn(PostLikeToggleRes.builder().postId(1L).liked(true).likeCount(1).build());

        mockMvc.perform(post("/api/trustay/posts/1/like")
                        .principal((Principal) () -> "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("POST /api/trustay/posts/99/like - 없는 게시글이면 4000")
    void toggleLike_postNotFound() throws Exception {
        when(postService.toggleLike("user@test.com", 99L))
                .thenThrow(new IllegalArgumentException("게시글을 찾을 수 없습니다."));

        mockMvc.perform(post("/api/trustay/posts/99/like")
                        .principal((Principal) () -> "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4000));
    }

    // ─────────────────────────────────────────────────────────────────────
    // 댓글
    // ─────────────────────────────────────────────────────────────────────

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("POST /api/trustay/posts/1/comments - 댓글 작성")
    void createComment_success() throws Exception {
        String body = "{\"content\":\"좋아요!\"}";

        when(commentService.create(eq("user@test.com"), eq(1L), any()))
                .thenReturn(CommentRes.builder().id(100L).content("좋아요!").build());

        mockMvc.perform(post("/api/trustay/posts/1/comments")
                        .principal((Principal) () -> "user@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.content").value("좋아요!"));
    }

    @Test
    @DisplayName("GET /api/trustay/posts/1/comments - 댓글 목록")
    void listComments_success() throws Exception {
        when(commentService.list(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        CommentRes.builder().id(1L).content("c1").build(),
                        CommentRes.builder().id(2L).content("c2").build()
                )));

        mockMvc.perform(get("/api/trustay/posts/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("PUT /api/trustay/posts/1/comments/100 - 댓글 수정")
    void updateComment_success() throws Exception {
        String body = "{\"content\":\"수정\"}";

        when(commentService.update(eq("user@test.com"), eq(1L), eq(100L), any()))
                .thenReturn(CommentRes.builder().id(100L).content("수정").build());

        mockMvc.perform(put("/api/trustay/posts/1/comments/100")
                        .principal((Principal) () -> "user@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("수정"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("PUT /api/trustay/posts/1/comments/999 - 없는 댓글이면 NOT_FOUND_COMMENT(4046)")
    void updateComment_notFound() throws Exception {
        String body = "{\"content\":\"수정\"}";

        when(commentService.update(eq("user@test.com"), eq(1L), eq(999L), any()))
                .thenThrow(new EntityNotFoundException("not found"));

        mockMvc.perform(put("/api/trustay/posts/1/comments/999")
                        .principal((Principal) () -> "user@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4046));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("PUT /api/trustay/posts/1/comments/100 - 본인 댓글 아니면 4030")
    void updateComment_forbidden() throws Exception {
        String body = "{\"content\":\"수정\"}";

        when(commentService.update(eq("user@test.com"), eq(1L), eq(100L), any()))
                .thenThrow(new IllegalStateException("본인이 작성한 댓글만 수정할 수 있습니다."));

        mockMvc.perform(put("/api/trustay/posts/1/comments/100")
                        .principal((Principal) () -> "user@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4030));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("DELETE /api/trustay/posts/1/comments/100 - 댓글 삭제(soft delete)")
    void deleteComment_success() throws Exception {
        mockMvc.perform(delete("/api/trustay/posts/1/comments/100")
                        .principal((Principal) () -> "user@test.com"))
                .andExpect(status().isOk());
        verify(commentService).delete("user@test.com", 1L, 100L);
    }

    @Test
    @WithMockUser(username = "user@test.com")
    @DisplayName("DELETE /api/trustay/posts/1/comments/999 - 없으면 4046")
    void deleteComment_notFound() throws Exception {
        doThrow(new EntityNotFoundException("not found"))
                .when(commentService).delete("user@test.com", 1L, 999L);

        mockMvc.perform(delete("/api/trustay/posts/1/comments/999")
                        .principal((Principal) () -> "user@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4046));
    }
}
