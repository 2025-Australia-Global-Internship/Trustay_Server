package com.maritel.trustay.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "댓글 작성 요청")
public class CommentReq {

    @NotBlank
    @Size(max = 1000)
    @Schema(description = "댓글 내용", example = "잘 봤습니다!")
    private String content;
}
