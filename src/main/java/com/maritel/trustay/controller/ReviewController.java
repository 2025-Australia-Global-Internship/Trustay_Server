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
@Tag(name = "Review API", description = "쉐어하우스 매물 리뷰/평점")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "리뷰 작성",
            description = "해당 매물의 거주 이력(Contract ACTIVE 또는 EXPIRED)이 있는 사용자만 작성 가능, 한 매물에 1회만 작성 가능합니다.")
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

    @Operation(summary = "리뷰 수정", description = "본인이 작성한 리뷰만 수정 가능합니다.")
    @PutMapping("/{reviewId}")
    public ResponseEntity<DataResponse<ReviewRes>> updateReview(
            Principal principal,
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewUpdateReq req) {
        try {
            ReviewRes res = reviewService.updateReview(principal.getName(), reviewId, req);
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, res));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.ok(DataResponse.of(ResponseCode.NOT_FOUND_REVIEW));
        } catch (IllegalStateException e) {
            return forbidden(e.getMessage());
        }
    }

    @Operation(summary = "리뷰 삭제", description = "본인이 작성한 리뷰만 삭제 가능합니다.")
    @DeleteMapping("/{reviewId}")
    public ResponseEntity<DataResponse<Void>> deleteReview(
            Principal principal,
            @PathVariable Long reviewId) {
        try {
            reviewService.deleteReview(principal.getName(), reviewId);
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.ok(DataResponse.of(ResponseCode.NOT_FOUND_REVIEW));
        } catch (IllegalStateException e) {
            return forbidden(e.getMessage());
        }
    }

    @Operation(summary = "매물 리뷰 목록 조회", description = "특정 쉐어하우스에 대한 리뷰 목록 (최신순, 페이징)")
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

    @Operation(summary = "매물 평점 요약", description = "특정 쉐어하우스의 평균 평점과 리뷰 수")
    @GetMapping("/house/{houseId}/summary")
    public ResponseEntity<DataResponse<ReviewService.RatingSummary>> getHouseRatingSummary(
            @PathVariable Long houseId) {
        ReviewService.RatingSummary summary = reviewService.getHouseRatingSummary(houseId);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, summary));
    }

    @Operation(summary = "내가 작성한 리뷰 목록")
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
}
