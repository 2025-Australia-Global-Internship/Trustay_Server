package com.maritel.trustay.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PaymentConfirmReq {

    @NotBlank
    @Schema(description = "paymentKey returned by the Toss payment widget.")
    private String paymentKey;

    @NotBlank
    @Schema(description = "orderId issued by the server during the prepare step.")
    private String orderId;

    @NotNull
    @Min(1)
    private Long amount;
}
