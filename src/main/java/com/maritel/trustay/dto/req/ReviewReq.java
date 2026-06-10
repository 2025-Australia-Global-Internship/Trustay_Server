package com.maritel.trustay.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "리뷰 작성 요청")
public class ReviewReq {

    @NotNull
    @Schema(description = "리뷰 대상 매물 ID", example = "1")
    private Long houseId;

    @NotNull
    @Min(1) @Max(5)
    @Schema(description = "평점 (1~5)", example = "5")
    private Integer rating;

    @Size(max = 2000)
    @Schema(description = "리뷰 내용 (최대 2000자)")
    private String content;
}
