package com.maritel.trustay.dto.req;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class CommunityReq {

    @NotBlank(message = "Community name is required.")
    private String name;

    private String description;

    private String imageUrl;
}
