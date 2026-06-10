package com.maritel.trustay.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class OAuthLoginReq {
    @NotBlank(message = "Token is required.")
    private String firebaseToken;
}
