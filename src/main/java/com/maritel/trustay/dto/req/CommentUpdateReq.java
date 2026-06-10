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
@Schema(description = "댓글 수정 요청")
public class CommentUpdateReq {

    @NotBlank
    @Size(max = 1000)
    @Schema(description = "수정할 댓글 내용")
    private String content;
}
