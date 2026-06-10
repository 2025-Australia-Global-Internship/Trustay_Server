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
@Schema(description = "자동이체 스케줄 생성 요청")
public class AutoTransferReq {

    @NotNull
    @Schema(description = "수취인(집주인 등) 회원 ID", example = "10")
    private Long payeeMemberId;

    @Schema(description = "연결할 계약 ID (선택)")
    private Long contractId;

    @NotNull
    @Min(1)
    @Schema(description = "금액", example = "500000")
    private Long amount;

    @NotNull
    @Schema(description = "결제 타입 (RENT 또는 UTILITY)", example = "RENT")
    private PaymentType type;

    @NotNull
    @Min(1) @Max(31)
    @Schema(description = "매월 결제일 (1~31)", example = "5")
    private Integer dayOfMonth;

    @Size(max = 200)
    @Schema(description = "메모", example = "5월 월세")
    private String memo;
}
