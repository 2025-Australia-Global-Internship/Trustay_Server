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
@Schema(description = "Request to register or refresh an FCM device token.")
public class FcmTokenReq {

    @NotBlank
    @Schema(description = "FCM device token.", example = "dQwT9...")
    private String token;

    @NotNull
    @Schema(description = "Device type.", example = "ANDROID")
    private DeviceType deviceType;
}
