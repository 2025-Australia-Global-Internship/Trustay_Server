package com.maritel.trustay.dto.req;

import com.maritel.trustay.constant.PaymentType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request to create an auto-transfer schedule.")
public class AutoTransferReq {

    @NotNull
    @Schema(description = "Payee member ID (e.g., the landlord).", example = "10")
    private Long payeeMemberId;

    @Schema(description = "Contract ID to link (optional).")
    private Long contractId;

    @NotNull
    @Min(1)
    @Schema(description = "Amount.", example = "500000")
    private Long amount;

    @NotNull
    @Schema(description = "Payment type (RENT or UTILITY).", example = "RENT")
    private PaymentType type;

    @NotNull
    @Min(1) @Max(31)
    @Schema(description = "Day of month for payment (1-31).", example = "5")
    private Integer dayOfMonth;

    @Size(max = 200)
    @Schema(description = "Memo.", example = "May rent")
    private String memo;
}
