package com.maritel.trustay.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class LoginResultRes {

    @Schema(description = "Login authentication token.")
    private String token;

}
