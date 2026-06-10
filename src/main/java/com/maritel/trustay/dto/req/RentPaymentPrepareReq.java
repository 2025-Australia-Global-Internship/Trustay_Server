package com.maritel.trustay.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RentPaymentPrepareReq {

    @NotNull
    @Schema(description = "Contract ID.")
    private Long contractId;

    @NotNull
    @Min(100)
    @Schema(description = "Payment amount (KRW). Toss test mode also typically requires at least 100.")
    private Long amount;
}
