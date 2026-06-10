package com.maritel.trustay.dto.req;

import com.maritel.trustay.constant.PatternConstants;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupReq {

    @NotBlank(message = "Name is required.")
    @Pattern(regexp = PatternConstants.NAME_REGEX, message = PatternConstants.NAME_MESSAGE)
    @Size(min = 2, max = 25, message = "Name must be between 2 and 25 characters.")
    private String name;

    @NotBlank(message = "Email is required.")
    @Pattern(regexp = PatternConstants.EMAIL_REGEX, message = PatternConstants.EMAIL_MESSAGE)
    @Size(max = 100, message = "Email must be 100 characters or fewer.")
    private String email;

    @NotBlank(message = "Password is required.")
    @Pattern(regexp = PatternConstants.PASSWORD_REGEX, message = PatternConstants.PASSWORD_MESSAGE)
    @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters.")
    private String passwd;

    // 회원가입 시 필수가 아닌 정보들은 제거 (프로필 수정 시 입력)
}