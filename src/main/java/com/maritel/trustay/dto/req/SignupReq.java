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

    @NotBlank(message = "이름은 필수 입력 값입니다.")
    @Pattern(regexp = PatternConstants.NAME_REGEX, message = PatternConstants.NAME_MESSAGE)
    @Size(min = 2, max = 25, message = "이름은 25자 이하, 2자 이상으로 입력해주세요.")
    private String name;

    @NotBlank(message = "이메일은 필수 입력 값입니다.")
    @Pattern(regexp = PatternConstants.EMAIL_REGEX, message = PatternConstants.EMAIL_MESSAGE)
    @Size(max = 100, message = "이메일은 100자 이하로 입력해주세요.")
    private String email;

    @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
    @Pattern(regexp = PatternConstants.PASSWORD_REGEX, message = PatternConstants.PASSWORD_MESSAGE)
    @Size(min = 8, max = 50, message = "비밀번호는 최소 8자 이상, 최대 50자 이하로 입력해주세요.")
    private String passwd;

    // 회원가입 시 필수가 아닌 정보들은 제거 (프로필 수정 시 입력)
}