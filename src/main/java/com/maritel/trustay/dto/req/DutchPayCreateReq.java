package com.maritel.trustay.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class DutchPayCreateReq {

    @NotNull
    @Min(1)
    @Schema(description = "Total amount to split (KRW).")
    private Long totalAmount;

    @NotNull
    @NotEmpty
    @Schema(description = "List of member IDs participating in the split (no duplicates). Must include the payee as well.")
    private List<Long> memberIds;

    @NotNull
    @Schema(description = "Member who will receive the funds (account owner). Must be included in memberIds.")
    private Long payeeMemberId;

    @Schema(description = "Memo or title.")
    private String title;

    @Schema(description = "Optional: related contract ID.")
    private Long contractId;
}
