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
@Schema(description = "Request to create a comment.")
public class CommentReq {

    @NotBlank
    @Size(max = 1000)
    @Schema(description = "Comment content.", example = "Nice post!")
    private String content;
}
