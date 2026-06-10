package com.maritel.trustay.dto.req;

import com.maritel.trustay.constant.PatternConstants;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ProfileUpdateReq {

    @Pattern(regexp = PatternConstants.BIRTH_REGEX, message = PatternConstants.BIRTH_MESSAGE)
    private String birth;

    @Pattern(regexp = PatternConstants.PHONE_REGEX, message = PatternConstants.PHONE_MESSAGE)
    private String phone;

    @Pattern(regexp = PatternConstants.ACCOUNT_REGEX, message = PatternConstants.ACCOUNT_MESSAGE)
    private String accountInfo;

    @Size(max = 25, message = "Gender must be 25 characters or fewer.")
    private String gender;
    @Size(max = 255, message = "Address must be 255 characters or fewer.")
    private String address;

    // 프로필 이미지는 MultipartFile로 별도 처리
}