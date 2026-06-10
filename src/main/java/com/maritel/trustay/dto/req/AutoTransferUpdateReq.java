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
@Schema(description = "자동이체 스케줄 수정 요청")
public class AutoTransferUpdateReq {

    @Min(1)
    @Schema(description = "변경할 금액")
    private Long amount;

    @Min(1) @Max(31)
    @Schema(description = "변경할 매월 결제일 (1~31)")
    private Integer dayOfMonth;

    @Size(max = 200)
    @Schema(description = "변경할 메모")
    private String memo;

    @Schema(description = "활성/비활성 토글")
    private Boolean active;
}
