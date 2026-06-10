package com.maritel.trustay.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request to update an auto-transfer schedule.")
public class AutoTransferUpdateReq {

    @Min(1)
    @Schema(description = "New amount.")
    private Long amount;

    @Min(1) @Max(31)
    @Schema(description = "New day of month for payment (1-31).")
    private Integer dayOfMonth;

    @Size(max = 200)
    @Schema(description = "New memo.")
    private String memo;

    @Schema(description = "Toggle active/inactive.")
    private Boolean active;
}
