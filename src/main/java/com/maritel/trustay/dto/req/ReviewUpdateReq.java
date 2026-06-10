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
@Schema(description = "Request to update a review.")
public class ReviewUpdateReq {

    @NotNull
    @Min(1) @Max(5)
    @Schema(description = "Rating (1-5).", example = "4")
    private Integer rating;

    @Size(max = 2000)
    @Schema(description = "Review content (up to 2000 characters).")
    private String content;
}
