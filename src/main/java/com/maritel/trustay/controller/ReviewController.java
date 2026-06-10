package com.maritel.trustay.controller;

import com.maritel.trustay.dto.req.ReviewReq;
import com.maritel.trustay.dto.req.ReviewUpdateReq;
import com.maritel.trustay.dto.res.DataResponse;
import com.maritel.trustay.dto.res.PageResponse;
import com.maritel.trustay.dto.res.ResponseCode;
import com.maritel.trustay.dto.res.ReviewRes;
import com.maritel.trustay.service.ReviewService;
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
@RequestMapping("/api/trustay/reviews")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Review API", description = "Sharehouse listing reviews and ratings.")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "Create a review.",
            description = "Only users with a residency history (Contract status ACTIVE or EXPIRED) for the listing can write a review, and only one review per listing is allowed.")
    @PostMapping
    public ResponseEntity<DataResponse<ReviewRes>> createReview(
            Principal principal,
            @Valid @RequestBody ReviewReq req) {
        try {
            ReviewRes res = reviewService.createReview(principal.getName(), req);
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, res));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(DataResponse.of(ResponseCode.NOT_VALID.getCode(), e.getMessage(), null));
        }
    }

    @Operation(summary = "Update a review.", description = "You can only edit reviews you wrote yourself.")
    @PutMapping("/{reviewId}")
    public ResponseEntity<DataResponse<ReviewRes>> updateReview(
            Principal principal,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateReq req) {
        try {
            ReviewRes res = reviewService.updateReview(principal.getName(), reviewId, req);
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, res));
        } catch (EntityNotFoundException e) {
            return error(ResponseCode.NOT_FOUND_REVIEW);
        } catch (IllegalStateException e) {
            return forbidden(e.getMessage());
        }
    }

    @Operation(summary = "Delete a review.", description = "You can only delete reviews you wrote yourself.")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<DataResponse<Void>> deleteReview(
            Principal principal,
            @PathVariable Long reviewId) {
        try {
            reviewService.deleteReview(principal.getName(), reviewId);
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS));
        } catch (EntityNotFoundException e) {
            return error(ResponseCode.NOT_FOUND_REVIEW);
        } catch (IllegalStateException e) {
            return forbidden(e.getMessage());
        }
    }

    @Operation(summary = "List reviews for a listing.", description = "Returns reviews for a specific sharehouse (most recent first, paginated).")
    @GetMapping("/house/{houseId}")
    public ResponseEntity<DataResponse<PageResponse<ReviewRes>>> getHouseReviews(
            @PathVariable Long houseId,
            @PageableDefault(size = 10, sort = "regTime", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            Page<ReviewRes> page = reviewService.getHouseReviews(houseId, pageable);
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, new PageResponse<>(page)));
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        }
    }

    @Operation(summary = "Get rating summary for a listing.", description = "Returns the average rating and review count for a specific sharehouse.")
    @GetMapping("/house/{houseId}/summary")
    public ResponseEntity<DataResponse<ReviewService.RatingSummary>> getHouseRatingSummary(
            @PathVariable Long houseId) {
        ReviewService.RatingSummary summary = reviewService.getHouseRatingSummary(houseId);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, summary));
    }

    @Operation(summary = "List my reviews.")
    @GetMapping("/me")
    public ResponseEntity<DataResponse<PageResponse<ReviewRes>>> getMyReviews(
            Principal principal,
            @PageableDefault(size = 10, sort = "regTime", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ReviewRes> page = reviewService.getMyReviews(principal.getName(), pageable);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, new PageResponse<>(page)));
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
