package com.maritel.trustay.dto.req;

import com.maritel.trustay.constant.PatternConstants;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class LoginReq {

    @NotBlank(message = "Email is required.")
    @Pattern(regexp = PatternConstants.EMAIL_REGEX, message = PatternConstants.EMAIL_MESSAGE)
    @Size(max = 100, message = "Email must be 100 characters or fewer.")
    private String email;

    @NotBlank(message = "Password is required.")
    @Pattern(regexp = PatternConstants.PASSWORD_REGEX, message = PatternConstants.PASSWORD_MESSAGE)
    @Size(min = 8, max = 50, message = "Password must be between 8 and 50 characters.")
    private String passwd;
}
