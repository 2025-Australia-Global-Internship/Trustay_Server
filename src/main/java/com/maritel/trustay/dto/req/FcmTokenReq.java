package com.maritel.trustay.dto.req;

import com.maritel.trustay.constant.DeviceType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "FCM 디바이스 토큰 등록/갱신 요청")
public class FcmTokenReq {

    @NotBlank
    @Schema(description = "FCM 디바이스 토큰", example = "dQwT9...")
    private String token;

    @NotNull
    @Schema(description = "디바이스 종류", example = "ANDROID")
    private DeviceType deviceType;
}
