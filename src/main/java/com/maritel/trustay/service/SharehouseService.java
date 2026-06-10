package com.maritel.trustay.service;

import com.maritel.trustay.constant.ApprovalStatus;
import com.maritel.trustay.constant.ContractStatus;
import com.maritel.trustay.constant.NotificationType;
import com.maritel.trustay.constant.Role;
import com.maritel.trustay.dto.req.SharehouseReq;
import com.maritel.trustay.dto.req.SharehouseSearchReq;
import com.maritel.trustay.dto.req.SharehouseUpdateReq;
import com.maritel.trustay.dto.res.SharehouseRes;
import com.maritel.trustay.dto.res.SharehouseResultRes;
import com.maritel.trustay.dto.res.WishToggleRes;
import com.maritel.trustay.entity.Contract;
import com.maritel.trustay.entity.Image;
import com.maritel.trustay.entity.Member;
import com.maritel.trustay.entity.Sharehouse;
import com.maritel.trustay.entity.SharehouseImage;
import com.maritel.trustay.entity.SharehouseWish;
import com.maritel.trustay.repository.ContractRepository;
import com.maritel.trustay.repository.ImageRepository;
import com.maritel.trustay.repository.MemberRepository;
import com.maritel.trustay.repository.ReviewRepository;
import com.maritel.trustay.repository.SharehouseImageRepository;
import com.maritel.trustay.repository.SharehouseRepository;
import com.maritel.trustay.repository.SharehouseWishRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SharehouseService {

    private final SharehouseRepository sharehouseRepository;
    private final MemberRepository memberRepository;
    private final ContractRepository contractRepository;
    private final GeocodingService geocodingService; // 1. 주입 추가
    private final ImageRepository imageRepository;
    private final SharehouseImageRepository sharehouseImageRepository;
    private final SharehouseWishRepository sharehouseWishRepository;
    private final NotificationService notificationService;
    private final ReviewRepository reviewRepository;


    /**
     * 내 목록에서 쉐어하우스 상세 조회하기
     */
    public SharehouseResultRes getMySharehouseDetail(@PathVariable Long houseId) {
        Sharehouse sharehouse = sharehouseRepository.findById(houseId)
                .orElseThrow(() -> new IllegalArgumentException("Sharehouse not found."));

        // [수정] 이미지 리스트 조회 후 함께 전달
        List<SharehouseImage> images = sharehouseImageRepository.findBySharehouseId(houseId);
        double avg = round2(reviewRepository.findAverageRatingByHouseId(houseId));
        long cnt = reviewRepository.countByTargetHouse_Id(houseId);
        return SharehouseResultRes.from(sharehouse, images, avg, cnt);
    }

    /**
     * 내가 등록한 쉐어하우스 목록 조회
     */
    public Page<SharehouseRes> getMySharehouseList(String email, Pageable pageable) {
        Page<Sharehouse> sharehouses = sharehouseRepository.findByHostEmail(email, pageable);

        Map<Long, double[]> ratingMap = aggregateRatings(sharehouses.map(Sharehouse::getId).toList());
        return sharehouses.map(sharehouse -> {
            List<SharehouseImage> images = sharehouseImageRepository.findBySharehouseId(sharehouse.getId());
            double[] r = ratingMap.getOrDefault(sharehouse.getId(), new double[]{0.0, 0.0});
            return SharehouseRes.from(sharehouse, images, false, r[0], (long) r[1]);
        });
    }

    /** Review 평점 집계 (houseId → [avgRating, reviewCount]) */
    private Map<Long, double[]> aggregateRatings(List<Long> houseIds) {
        if (houseIds == null || houseIds.isEmpty()) return Map.of();
        List<Object[]> rows = reviewRepository.aggregateByHouseIds(houseIds);
        Map<Long, double[]> result = new HashMap<>();
        for (Object[] row : rows) {
            Long id = ((Number) row[0]).longValue();
            double avg = row[1] != null ? round2(((Number) row[1]).doubleValue()) : 0.0;
            double cnt = row[2] != null ? ((Number) row[2]).doubleValue() : 0.0;
            result.put(id, new double[]{avg, cnt});
        }
        return result;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }



    @Transactional
    public SharehouseRes registerSharehouse(String userEmail, SharehouseReq req) {
        Member host = memberRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        // 매물 등록 시 TENANT 권한만 가진 사용자는 HOST 권한도 함께 부여
        if (host.getProfile() != null) {
            Set<Role> roles = host.getProfile().getRoles();
            if (roles.size() == 1 && roles.contains(Role.TENANT)) {
                host.getProfile().addRole(Role.HOST);
            }
        }

        Map<String, Double> coords = geocodingService.getCoordinates(req.getAddress());
        Double latitude = (coords != null) ? coords.get("lat") : 0.0;
        Double longitude = (coords != null) ? coords.get("lon") : 0.0;

        // [변경] 더 이상 String imageUrls를 쓰지 않음
        String homeRulesString = (req.getHomeRules() != null) ? String.join(",", req.getHomeRules()) : "";
        String featuresString = (req.getFeatures() != null) ? String.join(",", req.getFeatures()) : "";

        Sharehouse sharehouse = Sharehouse.builder()
                .host(host)
                .title(req.getTitle())
                .description(req.getDescription())
                .address(req.getAddress())
                .latitude(latitude)
                .longitude(longitude)
                .houseType(req.getHouseType())
                .rentPrice(req.getRentPrice())
                .roomCount(req.getRoomCount())
                .bathroomCount(req.getBathroomCount())
                .currentResidents(req.getCurrentResidents())
                .homeRules(homeRulesString)
                .features(featuresString)
                .billsIncluded(req.getBillsIncluded())
                .roomType(req.getRoomType())
                .bondType(req.getBondType())
                .minimumStay(req.getMinimumStay())
                .gender(req.getGender())
                .age(req.getAge())
                .religion(req.getReligion())
                .dietaryPreference(req.getDietaryPreference())
                .approvalStatus(ApprovalStatus.PENDING)
                .build();

        Sharehouse savedHouse = sharehouseRepository.save(sharehouse);
        List<SharehouseImage> savedImages = new java.util.ArrayList<>();

        if (req.getImageUrls() != null) {
            for (String url : req.getImageUrls()) {
                Image image = imageRepository.save(Image.builder().imageUrl(url).build());

                SharehouseImage si = sharehouseImageRepository.save(SharehouseImage.builder()
                        .sharehouse(savedHouse)
                        .image(image)
                        .build());
                savedImages.add(si); // 저장된 이미지 객체들을 리스트에 담음
            }
        }

        return SharehouseRes.from(savedHouse, savedImages);
    }


    /**
     * 1. 쉐어하우스 수정
     * - 작성자 본인인지 확인 후 수정
     */
    @Transactional
    public void updateSharehouse(Long houseId, String email, SharehouseUpdateReq req) {
        Sharehouse sharehouse = sharehouseRepository.findById(houseId)
                .orElseThrow(() -> new IllegalArgumentException("Sharehouse not found."));

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (!sharehouse.getHost().getEmail().equals(email) && !isAdmin(member)) {
            throw new IllegalStateException("You don't have permission to edit this listing.");
        }

        sharehouse.updateSharehouse(
                req.getTitle(), req.getDescription(), req.getRentPrice(), req.getHomeRules(),req.getFeatures(), req.getRoomCount(),
                 req.getBathroomCount(), req.getCurrentResidents(), req.getHouseType(),
                req.getBillsIncluded(), req.getRoomType(), req.getBondType(), req.getMinimumStay(),
                req.getGender(), req.getAge(), req.getReligion(), req.getDietaryPreference()
        );
    }

    /**
     * 2. 쉐어하우스 삭제
     * - 작성자 본인인지 확인 후 삭제
     */
    @Transactional
    public void deleteSharehouse(Long houseId, String email) {
        Sharehouse sharehouse = sharehouseRepository.findById(houseId)
                .orElseThrow(() -> new IllegalArgumentException("Sharehouse not found."));

        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        if (!sharehouse.getHost().getEmail().equals(email) && !this.isAdmin(member)) {
            throw new IllegalStateException("You don't have permission to delete this listing.");
        }

        sharehouseRepository.delete(sharehouse);
    }

    /**
     * 3. 쉐어하우스 승인/거절 (관리자용)
     */
    @Transactional
    public void approveSharehouse(Long houseId, ApprovalStatus status, String adminEmail) {

        // 1. 요청자(관리자) 조회
        Member admin = memberRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        // 2. 권한 확인 (Profile 테이블의 Role 확인)
        // Profile이 없거나, Role이 ADMIN이 아니면 예외 발생
        log.info(admin.getEmail());
        if (admin.getProfile() == null || !isAdmin(admin)) {
            throw new IllegalStateException("Admin permission is required.");
        }

        // 3. 매물 조회 및 상태 변경
        Sharehouse sharehouse = sharehouseRepository.findById(houseId)
                .orElseThrow(() -> new IllegalArgumentException("Sharehouse not found."));

        sharehouse.changeApprovalStatus(status);

        // 4. 집주인(host)에게 승인/거절 알림 발행
        String title = status == ApprovalStatus.ACTIVE
                ? "Your listing has been approved."
                : status == ApprovalStatus.REJECTED
                    ? "Your listing has been rejected."
                    : "Your listing status has changed.";
        notificationService.notify(
                sharehouse.getHost(),
                NotificationType.APPROVAL,
                title,
                String.format("Your listing \"%s\" is now %s.",
                        sharehouse.getTitle(), status.name()),
                "/sharehouse/" + sharehouse.getId()
        );
    }

    /**
     * [수정] 쉐어하우스 상세 조회
     * - 조회 시 viewCount 증가
     * - 상세 정보인 SharehouseResultRes 반환
     */
    @Transactional
    public SharehouseResultRes getSharehouseDetail(Long houseId) {
        sharehouseRepository.updateViewCount(houseId);

        Sharehouse sharehouse = sharehouseRepository.findById(houseId)
                .orElseThrow(() -> new IllegalArgumentException("Sharehouse not found."));

        // [추가] 해당 쉐어하우스의 이미지 리스트 조회
        List<SharehouseImage> images = sharehouseImageRepository.findBySharehouseId(houseId);
        double avg = round2(reviewRepository.findAverageRatingByHouseId(houseId));
        long cnt = reviewRepository.countByTargetHouse_Id(houseId);
        return SharehouseResultRes.from(sharehouse, images, avg, cnt);
    }

    /**
     * [수정] 쉐어하우스 목록 조회 (검색 + 페이징 + 정렬)
     */
    public Page<SharehouseRes> getSharehouseList(SharehouseSearchReq req, Pageable pageable) {
        Page<Sharehouse> sharehousePage = sharehouseRepository.searchSharehouses(req, pageable);

        Map<Long, double[]> ratingMap = aggregateRatings(sharehousePage.map(Sharehouse::getId).toList());
        return sharehousePage.map(sharehouse -> {
            List<SharehouseImage> images = sharehouseImageRepository.findBySharehouseId(sharehouse.getId());
            double[] r = ratingMap.getOrDefault(sharehouse.getId(), new double[]{0.0, 0.0});
            return SharehouseRes.from(sharehouse, images, false, r[0], (long) r[1]);
        });
    }

    /**
     * 쉐어하우스 찜하기/찜 해제 (토글)
     */
    @Transactional
    public WishToggleRes toggleWish(String userEmail, Long houseId) {
        Member member = memberRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        Sharehouse sharehouse = sharehouseRepository.findById(houseId)
                .orElseThrow(() -> new IllegalArgumentException("Sharehouse not found."));

        var existing = sharehouseWishRepository.findByMember_IdAndSharehouse_Id(member.getId(), sharehouse.getId());
        if (existing.isPresent()) {
            sharehouseWishRepository.delete(existing.get());
            sharehouse.decreaseWishCount();
            sharehouseRepository.save(sharehouse);
            return WishToggleRes.builder().sharehouseId(houseId).wished(false).build();
        } else {
            sharehouseWishRepository.save(SharehouseWish.builder()
                    .member(member)
                    .sharehouse(sharehouse)
                    .build());
            sharehouse.increaseWishCount();
            sharehouseRepository.save(sharehouse);
            return WishToggleRes.builder().sharehouseId(houseId).wished(true).build();
        }
    }

    /**
     * 내가 찜한 쉐어하우스 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<SharehouseRes> getMyWishlist(String userEmail, Pageable pageable) {
        Member member = memberRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        Page<SharehouseWish> wishes = sharehouseWishRepository.findByMember_IdOrderByRegTimeDesc(member.getId(), pageable);
        Map<Long, double[]> ratingMap = aggregateRatings(
                wishes.map(w -> w.getSharehouse().getId()).toList());
        return wishes.map(w -> {
            Sharehouse sh = w.getSharehouse();
            List<SharehouseImage> images = sharehouseImageRepository.findBySharehouseId(sh.getId());
            double[] r = ratingMap.getOrDefault(sh.getId(), new double[]{0.0, 0.0});
            return SharehouseRes.from(sh, images, true, r[0], (long) r[1]);
        });
    }

    @Transactional(readOnly = true)
    public SharehouseRes getMyCurrentSharehouse(String userEmail) {
        Member member = memberRepository.findByEmail(userEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        List<Contract> contracts = contractRepository.findByTenantEmailAndStatusOrderByRegTimeDesc(
                member.getEmail(),
                ContractStatus.ACTIVE
        );

        Contract currentContract = contracts.stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("You don't have a current residence."));

        Sharehouse sharehouse = currentContract.getSharehouse();
        List<SharehouseImage> images = sharehouseImageRepository.findBySharehouseId(sharehouse.getId());
        double avg = round2(reviewRepository.findAverageRatingByHouseId(sharehouse.getId()));
        long cnt = reviewRepository.countByTargetHouse_Id(sharehouse.getId());
        return SharehouseRes.from(sharehouse, images, false, avg, cnt);
    }

    private boolean isAdmin(Member member) {
        return (member.getProfile().getRoles().contains(Role.ADMIN)); // 또는 member.getRole() == Role.ADMIN
    }
}