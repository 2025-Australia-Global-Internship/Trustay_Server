package com.maritel.trustay.service;

import com.maritel.trustay.constant.ContractStatus;
import com.maritel.trustay.constant.NotificationType;
import com.maritel.trustay.dto.req.ReviewReq;
import com.maritel.trustay.dto.req.ReviewUpdateReq;
import com.maritel.trustay.dto.res.ReviewRes;
import com.maritel.trustay.entity.Contract;
import com.maritel.trustay.entity.Member;
import com.maritel.trustay.entity.Review;
import com.maritel.trustay.entity.Sharehouse;
import com.maritel.trustay.repository.ContractRepository;
import com.maritel.trustay.repository.MemberRepository;
import com.maritel.trustay.repository.ReviewRepository;
import com.maritel.trustay.repository.SharehouseRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;
    private final SharehouseRepository sharehouseRepository;
    private final ContractRepository contractRepository;
    private final NotificationService notificationService;

    // ────────────────────────────────────────────────────────────────────────
    // 작성
    // ────────────────────────────────────────────────────────────────────────

    @Transactional
    public ReviewRes createReview(String email, ReviewReq req) {
        Member author = findMember(email);
        Sharehouse house = sharehouseRepository.findById(req.getHouseId())
                .orElseThrow(() -> new IllegalArgumentException("Sharehouse not found."));

        // 권한: 해당 매물에 본인이 tenant 인 Contract (ACTIVE 또는 EXPIRED) 보유자만 작성 가능
        if (!hasEligibleStay(email, house.getId())) {
            throw new IllegalStateException("Only users with a residency history (contract) for this listing can leave a review.");
        }

        // 1인 1회 제한
        if (reviewRepository.existsByAuthor_IdAndTargetHouse_Id(author.getId(), house.getId())) {
            throw new IllegalStateException("You've already written a review for this listing.");
        }

        Review review = Review.builder()
                .author(author)
                .targetHouse(house)
                .rating(req.getRating())
                .content(req.getContent())
                .build();
        Review saved = reviewRepository.save(review);

        // 집주인에게 알림
        notificationService.notifyExcludingSender(
                house.getHost(),
                author.getId(),
                NotificationType.SYSTEM,
                "New review on your listing",
                String.format("A %d-star review was posted on \"%s\".", req.getRating(), house.getTitle()),
                "/sharehouse/" + house.getId()
        );

        return ReviewRes.from(saved);
    }

    private boolean hasEligibleStay(String email, Long houseId) {
        // ACTIVE 또는 EXPIRED 상태 중 해당 houseId 매물과 연결된 계약이 있는지 확인
        List<Contract> activeContracts = contractRepository.findByTenantEmailAndStatusOrderByRegTimeDesc(
                email, ContractStatus.ACTIVE);
        List<Contract> expiredContracts = contractRepository.findByTenantEmailAndStatusOrderByRegTimeDesc(
                email, ContractStatus.EXPIRED);
        return activeContracts.stream().anyMatch(c -> c.getSharehouse().getId().equals(houseId))
                || expiredContracts.stream().anyMatch(c -> c.getSharehouse().getId().equals(houseId));
    }

    // ────────────────────────────────────────────────────────────────────────
    // 수정 / 삭제
    // ────────────────────────────────────────────────────────────────────────

    @Transactional
    public ReviewRes updateReview(String email, Long reviewId, ReviewUpdateReq req) {
        Member me = findMember(email);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found."));
        if (!review.getAuthor().getId().equals(me.getId())) {
            throw new IllegalStateException("You can only edit reviews you wrote yourself.");
        }
        review.update(req.getRating(), req.getContent());
        return ReviewRes.from(review);
    }

    @Transactional
    public void deleteReview(String email, Long reviewId) {
        Member me = findMember(email);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("Review not found."));
        if (!review.getAuthor().getId().equals(me.getId())) {
            throw new IllegalStateException("You can only delete reviews you wrote yourself.");
        }
        reviewRepository.delete(review);
    }

    // ────────────────────────────────────────────────────────────────────────
    // 조회
    // ────────────────────────────────────────────────────────────────────────

    public Page<ReviewRes> getHouseReviews(Long houseId, Pageable pageable) {
        if (!sharehouseRepository.existsById(houseId)) {
            throw new IllegalArgumentException("Sharehouse not found.");
        }
        return reviewRepository.findByTargetHouseId(houseId, pageable).map(ReviewRes::from);
    }

    public Page<ReviewRes> getMyReviews(String email, Pageable pageable) {
        Member me = findMember(email);
        return reviewRepository.findByAuthorId(me.getId(), pageable).map(ReviewRes::from);
    }

    /** 매물 한 건의 평균 평점/리뷰 수 (소수점 둘째 자리까지) */
    public RatingSummary getHouseRatingSummary(Long houseId) {
        double avg = reviewRepository.findAverageRatingByHouseId(houseId);
        long count = reviewRepository.countByTargetHouse_Id(houseId);
        return new RatingSummary(houseId, round2(avg), count);
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private Member findMember(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
    }

    public record RatingSummary(Long houseId, double averageRating, long reviewCount) {}
}
