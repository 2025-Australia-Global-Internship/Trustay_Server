package com.maritel.trustay.controller;

import org.springframework.web.bind.annotation.RequestBody;
import com.maritel.trustay.constant.ApprovalStatus;
import com.maritel.trustay.dto.req.SharehouseReq;
import com.maritel.trustay.dto.req.SharehouseSearchReq;
import com.maritel.trustay.dto.req.SharehouseUpdateReq;
import com.maritel.trustay.dto.res.*;
import com.maritel.trustay.service.FileService;
import com.maritel.trustay.service.SharehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

// [중요] 여기 Import가 잘못되어 있었습니다. 아래 것으로 바꿔야 합니다.
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/trustay/sharehouses")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sharehouse API", description = "Manage sharehouse listings.")
public class SharehouseController {

    private final SharehouseService sharehouseService;
    private final FileService fileService; // [추가] 컨트롤러에서 직접 파일 저장 호출

    @Operation(summary = "Upload listing images.", description = "Uploads images to the server and returns the list of URLs.")
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DataResponse<List<String>>> uploadSharehouseImages(
            @RequestPart("images") List<MultipartFile> images
    ) throws IOException {

        List<String> uploadedUrls = new ArrayList<>();
        for (MultipartFile image : images) {
            // FileService의 기존 메서드 활용
            String url = fileService.uploadFile(image);
            uploadedUrls.add(url);
        }
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, uploadedUrls));
    }

    @Operation(summary = "Register a sharehouse listing.", description = "Registers a listing using the image URLs returned from the upload step.")
    @PostMapping("") // consumes 삭제 (이제 JSON만 받음)
    public ResponseEntity<DataResponse<SharehouseRes>> registerSharehouse(
            Principal principal,
            @Valid @RequestBody SharehouseReq req // [핵심] @RequestPart -> @RequestBody 변경
    ) {
        // ObjectMapper 변환 과정 삭제됨 -> 스프링이 알아서 해줌
        String userEmail = principal.getName();

        // 파일 없이 DTO만 넘김
        SharehouseRes response = sharehouseService.registerSharehouse(userEmail, req);

        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, response));
    }

    @Operation(summary = "Update a sharehouse listing.", description = "The host updates their own listing information.")
    @PutMapping("/{houseId}")
    public ResponseEntity<DataResponse<Void>> updateSharehouse(
            @PathVariable Long houseId,
            @RequestBody SharehouseUpdateReq req,
            Principal principal) {

        String email = principal.getName();
        sharehouseService.updateSharehouse(houseId, email, req);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS));
    }

    @Operation(summary = "Delete a sharehouse listing.", description = "The host deletes their own listing.")
    @DeleteMapping("/{houseId}")
    public ResponseEntity<DataResponse<Void>> deleteSharehouse(
            @PathVariable Long houseId,
            Principal principal) {

        String email = principal.getName();
        sharehouseService.deleteSharehouse(houseId, email);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS));
    }

    @Operation(summary = "Change a listing's approval status.", description = "Admins can approve (ACTIVE) or reject (REJECTED) a listing.")
    @PatchMapping("/{houseId}/approval")
    public ResponseEntity<DataResponse<Void>> approveSharehouse(
            @PathVariable Long houseId,
            @RequestParam ApprovalStatus status,
            Principal principal) { // Principal 추가

        // 로그인한 사용자(관리자 추정)의 이메일 추출
        String email = principal.getName();
        log.info(email);

        // 서비스에 이메일 전달하여 권한 체크 및 승인 수행
        sharehouseService.approveSharehouse(houseId, status, email);

        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS));
    }

    @Operation(summary = "View details of my sharehouse listing.", description = "Returns listing details without incrementing the view count.")
    @GetMapping("/my/{houseId}")
    public ResponseEntity<DataResponse<SharehouseResultRes>> getMySharehouseDetail(@PathVariable Long houseId) {
        SharehouseResultRes response = sharehouseService.getMySharehouseDetail(houseId);

        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, response));
    }

    @Operation(summary = "List my sharehouse listings.", description = "Returns the listings registered by the current host.")
    @GetMapping("/my")
    public ResponseEntity<DataResponse<PageResponse<SharehouseRes>>> getMySharehouses(
            Principal principal,
            // sort를 "id"로 변경, 필요에 따라 direction(DESC/ASC)을 설정하세요.
            @PageableDefault(size = 10, sort = "viewCount", direction = Sort.Direction.DESC) Pageable pageable) {

        String email = principal.getName();

        // 서비스 호출 (SharehouseService의 메서드)
        Page<SharehouseRes> resultPage = sharehouseService.getMySharehouseList(email, pageable);

        // PageResponse로 변환하여 반환
        PageResponse<SharehouseRes> response = new PageResponse<>(resultPage);

        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, response));
    }

    @Operation(summary = "Get sharehouse listing details.", description = "Returns the listing's details and increments the view count by one.")
    @GetMapping("/{houseId}")
    public ResponseEntity<DataResponse<SharehouseResultRes>> getSharehouseDetail(@PathVariable Long houseId) {
        SharehouseResultRes response = sharehouseService.getSharehouseDetail(houseId);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, response));
    }

    @Operation(summary = "Get my current residence.", description = "Returns the listing the current tenant is currently living in.")
    @GetMapping("/me/current")
    public ResponseEntity<DataResponse<SharehouseRes>> getMyCurrentSharehouse(Principal principal) {
        String email = principal.getName();
        SharehouseRes response = sharehouseService.getMyCurrentSharehouse(email);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, response));
    }

    @Operation(summary = "Toggle a sharehouse wishlist entry.", description = "Adds or removes the listing from the wishlist (toggle).")
    @PostMapping("/{houseId}/wish")
    public ResponseEntity<DataResponse<WishToggleRes>> toggleWish(
            Principal principal,
            @PathVariable Long houseId) {
        String email = principal.getName();
        WishToggleRes response = sharehouseService.toggleWish(email, houseId);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, response));
    }

    @Operation(summary = "List my wishlisted sharehouses.", description = "Returns the listings the current user has wishlisted.")
    @GetMapping("/wishlist")
    public ResponseEntity<DataResponse<PageResponse<SharehouseRes>>> getMyWishlist(
            Principal principal,
            @PageableDefault(size = 10, sort = "regTime", direction = Sort.Direction.DESC) Pageable pageable) {
        String email = principal.getName();
        Page<SharehouseRes> resultPage = sharehouseService.getMyWishlist(email, pageable);
        PageResponse<SharehouseRes> response = new PageResponse<>(resultPage);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, response));
    }

    @Operation(summary = "List sharehouse listings.")
    @GetMapping
    public ResponseEntity<DataResponse<PageResponse<SharehouseRes>>> getSharehouseList(
            @ModelAttribute SharehouseSearchReq req,
            @PageableDefault(size = 10, sort = "viewCount", direction = Sort.Direction.DESC) Pageable pageable) {

        // 1. 서비스에서 반환하는 타입인 Page<SharehouseRes>에 맞춰 변수 타입 수정
        Page<SharehouseRes> resultPage = sharehouseService.getSharehouseList(req, pageable);

        // 2. PageResponse의 제네릭 타입도 SharehouseRes로 수정
        PageResponse<SharehouseRes> response = new PageResponse<>(resultPage);

        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, response));
    }
}