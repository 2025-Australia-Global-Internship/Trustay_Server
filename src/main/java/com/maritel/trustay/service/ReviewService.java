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
                .orElseThrow(() -> new IllegalArgumentException("해당 쉐어하우스가 존재하지 않습니다."));

        // 권한: 해당 매물에 본인이 tenant 인 Contract (ACTIVE 또는 EXPIRED) 보유자만 작성 가능
        if (!hasEligibleStay(email, house.getId())) {
            throw new IllegalStateException("해당 매물의 거주 이력(계약)이 있는 사용자만 리뷰를 작성할 수 있습니다.");
        }

        // 1인 1회 제한
        if (reviewRepository.existsByAuthor_IdAndTargetHouse_Id(author.getId(), house.getId())) {
            throw new IllegalStateException("이미 해당 매물에 리뷰를 작성하였습니다.");
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
                "내 매물에 새 리뷰가 등록되었습니다.",
                String.format("[%s] %d점 후기가 작성되었습니다.", house.getTitle(), req.getRating()),
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
                .orElseThrow(() -> new EntityNotFoundException("리뷰를 찾을 수 없습니다."));
        if (!review.getAuthor().getId().equals(me.getId())) {
            throw new IllegalStateException("본인이 작성한 리뷰만 수정할 수 있습니다.");
        }
        review.update(req.getRating(), req.getContent());
        return ReviewRes.from(review);
    }

    @Transactional
    public void deleteReview(String email, Long reviewId) {
        Member me = findMember(email);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new EntityNotFoundException("리뷰를 찾을 수 없습니다."));
        if (!review.getAuthor().getId().equals(me.getId())) {
            throw new IllegalStateException("본인이 작성한 리뷰만 삭제할 수 있습니다.");
        }
        reviewRepository.delete(review);
    }

    // ────────────────────────────────────────────────────────────────────────
    // 조회
    // ────────────────────────────────────────────────────────────────────────

    public Page<ReviewRes> getHouseReviews(Long houseId, Pageable pageable) {
        if (!sharehouseRepository.existsById(houseId)) {
            throw new IllegalArgumentException("해당 쉐어하우스가 존재하지 않습니다.");
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
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
    }

    public record RatingSummary(Long houseId, double averageRating, long reviewCount) {}
}
