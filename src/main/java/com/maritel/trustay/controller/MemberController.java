package com.maritel.trustay.controller;

import com.maritel.trustay.dto.req.SignupReq;
import com.maritel.trustay.dto.req.ProfileUpdateReq;
import com.maritel.trustay.dto.res.DataResponse;
import com.maritel.trustay.dto.res.ProfileRes;
import com.maritel.trustay.dto.res.ResponseCode;
import com.maritel.trustay.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;

@RestController
@RequestMapping("/api/trustay/members")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Member API", description = "회원 가입 및 프로필 관리")
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    public ResponseEntity<DataResponse<Void>> signup(@Valid @RequestBody SignupReq requestDto) {
        try {
            memberService.signup(requestDto);
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(DataResponse.of(ResponseCode.ALREADY_EXIST_USER_EMAIL, null));
        }
    }

    @Operation(summary = "내 프로필 조회")
    @GetMapping("/profile")
    public ResponseEntity<DataResponse<ProfileRes>> getProfile(Principal principal) {
        String email = principal.getName();
        ProfileRes response = memberService.getProfile(email);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, response));
    }

    @Operation(summary = "내 프로필 정보 수정 (전화번호, 생일, 계좌)")
    @PatchMapping("/profile")
    public ResponseEntity<DataResponse<Void>> updateProfile(
            Principal principal,
            @Valid @RequestBody ProfileUpdateReq requestDto) {

        String email = principal.getName();
        memberService.updateProfileInfo(email, requestDto);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS));
    }

    @Operation(summary = "프로필 이미지 업로드")
    @PostMapping(value = "/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DataResponse<Void>> updateProfileImage(
            Principal principal,
            @RequestPart(value = "profileImage") MultipartFile profileImage) {

        String email = principal.getName();
        memberService.updateProfileImage(email, profileImage);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS));
    }
}