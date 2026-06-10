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
@Tag(name = "Post API", description = "Manage posts.")
public class PostController {

    private final PostService postService;
    private final CommentService commentService;

    @Operation(summary = "Create a post.", description = "Create a post in a regular community or a sharehouse community.")
    @PostMapping
    public ResponseEntity<DataResponse<PostRes>> createPost(
            Principal principal,
            @Valid @RequestBody PostReq req) {

        String userEmail = principal.getName();
        PostRes response = postService.createPost(userEmail, req);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, response));
    }

    @Operation(summary = "Get post details.", description = "Returns the post's details and increments the view count.")
    @GetMapping("/{postId}")
    public ResponseEntity<DataResponse<PostRes>> getPostDetail(
            @PathVariable Long postId,
            Principal principal) {
        String viewerEmail = principal != null ? principal.getName() : null;
        PostRes response = postService.getPostDetail(postId, viewerEmail);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, response));
    }

    @Operation(summary = "List my posts.", description = "Returns posts written by the current user.")
    @GetMapping("/me")
    public ResponseEntity<DataResponse<PageResponse<PostRes>>> getMyPosts(
            Principal principal,
            @PageableDefault(size = 10, sort = "regTime", direction = Sort.Direction.DESC) Pageable pageable) {
        String userEmail = principal.getName();
        Page<PostRes> resultPage = postService.getMyPosts(userEmail, pageable);
        PageResponse<PostRes> response = new PageResponse<>(resultPage);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, response));
    }

    @Operation(summary = "List posts in a regular community.", description = "Returns posts in a regular community (notices first, then most recent).")
    @GetMapping("/community/{communityId}")
    public ResponseEntity<DataResponse<PageResponse<PostRes>>> getCommunityPosts(
            @PathVariable Long communityId,
            @PageableDefault(size = 10, sort = "regTime", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<PostRes> resultPage = postService.getCommunityPosts(communityId, pageable);
        PageResponse<PostRes> response = new PageResponse<>(resultPage);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, response));
    }

    @Operation(summary = "List posts in a sharehouse community.", description = "Returns posts in a sharehouse community (notices first, then most recent).")
    @GetMapping("/sharehouse/{sharehouseId}")
    public ResponseEntity<DataResponse<PageResponse<PostRes>>> getSharehouseCommunityPosts(
            @PathVariable Long sharehouseId,
            @PageableDefault(size = 10, sort = "regTime", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<PostRes> resultPage = postService.getSharehouseCommunityPosts(sharehouseId, pageable);
        PageResponse<PostRes> response = new PageResponse<>(resultPage);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, response));
    }

    @Operation(summary = "Posts for you feed.", description = "Returns all regular community posts, most recent first.")
    @GetMapping("/feed")
    public ResponseEntity<DataResponse<PageResponse<PostRes>>> getAllPosts(
            @PageableDefault(size = 10, sort = "regTime", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<PostRes> resultPage = postService.getAllPosts(pageable);
        PageResponse<PostRes> response = new PageResponse<>(resultPage);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, response));
    }

    @Operation(summary = "Update a post.", description = "Only the author can edit posts in a regular community; only the host can edit posts in a sharehouse community.")
    @PutMapping("/{postId}")
    public ResponseEntity<DataResponse<Void>> updatePost(
            Principal principal,
            @PathVariable Long postId,
            @Valid @RequestBody PostUpdateReq req) {

        String userEmail = principal.getName();
        postService.updatePost(userEmail, postId, req);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS));
    }

    @Operation(summary = "Delete a post.", description = "Only the author can delete posts in a regular community; only the host can delete posts in a sharehouse community.")
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

    @Operation(summary = "Toggle a post like.", description = "If already liked, removes the like; otherwise adds one.")
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

    @Operation(summary = "Create a comment.")
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

    @Operation(summary = "List comments (in creation order, paginated).")
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

    @Operation(summary = "Update a comment.", description = "You can only edit comments you wrote yourself.")
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

    @Operation(summary = "Delete a comment (soft delete).", description = "You can only delete comments you wrote yourself.")
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
