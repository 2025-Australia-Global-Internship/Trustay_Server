package com.maritel.trustay.controller;

import com.maritel.trustay.dto.req.CommentReq;
import com.maritel.trustay.dto.req.CommentUpdateReq;
import com.maritel.trustay.dto.req.PostReq;
import com.maritel.trustay.dto.req.PostUpdateReq;
import com.maritel.trustay.dto.res.*;
import com.maritel.trustay.service.CommentService;
import com.maritel.trustay.service.PostService;
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

@RestController
@RequestMapping("/api/trustay/posts")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Post API", description = "게시글 관리")
public class PostController {

    private final PostService postService;
    private final CommentService commentService;

    @Operation(summary = "게시글 작성", description = "일반 커뮤니티 또는 쉐어하우스 커뮤니티에 게시글을 작성합니다.")
    @PostMapping
    public ResponseEntity<DataResponse<PostRes>> createPost(
            Principal principal,
            @Valid @RequestBody PostReq req) {

        String userEmail = principal.getName();
        PostRes response = postService.createPost(userEmail, req);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, response));
    }

    @Operation(summary = "게시글 상세 조회", description = "게시글 상세 정보를 조회합니다. (조회수 증가)")
    @GetMapping("/{postId}")
    public ResponseEntity<DataResponse<PostRes>> getPostDetail(
            @PathVariable Long postId,
            Principal principal) {
        String viewerEmail = principal != null ? principal.getName() : null;
        PostRes response = postService.getPostDetail(postId, viewerEmail);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, response));
    }

    @Operation(summary = "내가 작성한 게시글 목록", description = "로그인한 사용자가 작성한 게시글을 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<DataResponse<PageResponse<PostRes>>> getMyPosts(
            Principal principal,
            @PageableDefault(size = 10, sort = "regTime", direction = Sort.Direction.DESC) Pageable pageable) {
        String userEmail = principal.getName();
        Page<PostRes> resultPage = postService.getMyPosts(userEmail, pageable);
        PageResponse<PostRes> response = new PageResponse<>(resultPage);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, response));
    }

    @Operation(summary = "일반 커뮤니티 게시글 목록", description = "일반 커뮤니티의 게시글 목록을 조회합니다. (공지 먼저, 최신순)")
    @GetMapping("/community/{communityId}")
    public ResponseEntity<DataResponse<PageResponse<PostRes>>> getCommunityPosts(
            @PathVariable Long communityId,
            @PageableDefault(size = 10, sort = "regTime", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<PostRes> resultPage = postService.getCommunityPosts(communityId, pageable);
        PageResponse<PostRes> response = new PageResponse<>(resultPage);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, response));
    }

    @Operation(summary = "쉐어하우스 커뮤니티 게시글 목록", description = "쉐어하우스 커뮤니티의 게시글 목록을 조회합니다. (공지 먼저, 최신순)")
    @GetMapping("/sharehouse/{sharehouseId}")
    public ResponseEntity<DataResponse<PageResponse<PostRes>>> getSharehouseCommunityPosts(
            @PathVariable Long sharehouseId,
            @PageableDefault(size = 10, sort = "regTime", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<PostRes> resultPage = postService.getSharehouseCommunityPosts(sharehouseId, pageable);
        PageResponse<PostRes> response = new PageResponse<>(resultPage);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, response));
    }

    @Operation(summary = "전체 게시글 피드", description = "모든 일반 커뮤니티 게시글을 최신순으로 조회합니다. (Posts for you)")
    @GetMapping("/feed")
    public ResponseEntity<DataResponse<PageResponse<PostRes>>> getAllPosts(
            @PageableDefault(size = 10, sort = "regTime", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<PostRes> resultPage = postService.getAllPosts(pageable);
        PageResponse<PostRes> response = new PageResponse<>(resultPage);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, response));
    }

    @Operation(summary = "게시글 수정", description = "게시글을 수정합니다. 일반 커뮤니티는 작성자만, 쉐어하우스는 집주인만 수정 가능합니다.")
    @PutMapping("/{postId}")
    public ResponseEntity<DataResponse<Void>> updatePost(
            Principal principal,
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateReq req) {

        String userEmail = principal.getName();
        postService.updatePost(userEmail, postId, req);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS));
    }

    @Operation(summary = "게시글 삭제", description = "게시글을 삭제합니다. 일반 커뮤니티는 작성자만, 쉐어하우스는 집주인만 삭제 가능합니다.")
    @DeleteMapping("/{postId}")
    public ResponseEntity<DataResponse<Void>> deletePost(
            Principal principal,
            @PathVariable Long postId) {

        String userEmail = principal.getName();
        postService.deletePost(userEmail, postId);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS));
    }

    // =========================================================================
    // 좋아요 토글
    // =========================================================================

    @Operation(summary = "게시글 좋아요 토글", description = "이미 좋아요면 취소, 아니면 좋아요 등록")
    @PostMapping("/{postId}/like")
    public ResponseEntity<DataResponse<PostLikeToggleRes>> toggleLike(
            Principal principal,
            @PathVariable Long postId) {
        try {
            PostLikeToggleRes res = postService.toggleLike(principal.getName(), postId);
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, res));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    // =========================================================================
    // 댓글
    // =========================================================================

    @Operation(summary = "댓글 작성")
    @PostMapping("/{postId}/comments")
    public ResponseEntity<DataResponse<CommentRes>> createComment(
            Principal principal,
            @PathVariable Long postId,
            @Valid @RequestBody CommentReq req) {
        try {
            CommentRes res = commentService.create(principal.getName(), postId, req);
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, res));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "댓글 목록 (등록순, 페이징)")
    @GetMapping("/{postId}/comments")
    public ResponseEntity<DataResponse<PageResponse<CommentRes>>> listComments(
            @PathVariable Long postId,
            @PageableDefault(size = 20, sort = "regTime", direction = Sort.Direction.ASC) Pageable pageable) {
        try {
            Page<CommentRes> page = commentService.list(postId, pageable);
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, new PageResponse<>(page)));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "댓글 수정", description = "본인이 작성한 댓글만 수정 가능합니다.")
    @PutMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<DataResponse<CommentRes>> updateComment(
            Principal principal,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateReq req) {
        try {
            CommentRes res = commentService.update(principal.getName(), postId, commentId, req);
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, res));
        } catch (EntityNotFoundException e) {
            return error(ResponseCode.NOT_FOUND_COMMENT);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (IllegalStateException e) {
            return forbidden(e.getMessage());
        }
    }

    @Operation(summary = "댓글 삭제 (soft delete)", description = "본인이 작성한 댓글만 삭제 가능합니다.")
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<DataResponse<Void>> deleteComment(
            Principal principal,
            @PathVariable Long postId,
            @PathVariable Long commentId) {
        try {
            commentService.delete(principal.getName(), postId, commentId);
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS));
        } catch (EntityNotFoundException e) {
            return error(ResponseCode.NOT_FOUND_COMMENT);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
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

    private static <T> ResponseEntity<DataResponse<T>> error(ResponseCode code) {
        return ResponseEntity.ok(DataResponse.of(code.getCode(), code.getMessage(), null));
    }
}
