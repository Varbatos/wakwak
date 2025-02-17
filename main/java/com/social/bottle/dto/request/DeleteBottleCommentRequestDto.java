package com.social.bottle.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
public class DeleteBottleCommentRequestDto {

    @NotNull(message = "bottle_id is required.")
    private Integer bottleId;

    @NotNull(message = "comment_id is required.")
    private Integer commentId;
}
