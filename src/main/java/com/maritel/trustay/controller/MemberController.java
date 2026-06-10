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
@Tag(name = "Member API", description = "Sign up and profile management.")
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "Sign up.")
    @PostMapping("/signup")
    public ResponseEntity<DataResponse<Void>> signup(@Valid @RequestBody SignupReq requestDto) {
        try {
            memberService.signup(requestDto);
            return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS));
        } catch (IllegalStateException e) {
            return ResponseEntity.ok(DataResponse.of(ResponseCode.ALREADY_EXIST_USER_EMAIL, null));
        }
    }

    @Operation(summary = "Get my profile.")
    @GetMapping("/profile")
    public ResponseEntity<DataResponse<ProfileRes>> getProfile(Principal principal) {
        String email = principal.getName();
        ProfileRes response = memberService.getProfile(email);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS, response));
    }

    @Operation(summary = "Update my profile (phone, birthday, account).")
    @PatchMapping("/profile")
    public ResponseEntity<DataResponse<Void>> updateProfile(
            Principal principal,
            @Valid @RequestBody ProfileUpdateReq requestDto) {

        String email = principal.getName();
        memberService.updateProfileInfo(email, requestDto);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS));
    }

    @Operation(summary = "Upload a profile image.")
    @PostMapping(value = "/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DataResponse<Void>> updateProfileImage(
            Principal principal,
            @RequestPart(value = "profileImage") MultipartFile profileImage) {

        String email = principal.getName();
        memberService.updateProfileImage(email, profileImage);
        return ResponseEntity.ok(DataResponse.of(ResponseCode.SUCCESS));
    }
}