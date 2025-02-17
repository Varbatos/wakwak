package com.social.friends.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchFriendStatusRequestDto {

    @NotNull(message = "Target user ID is required.")
    private Integer targetUserId;
}
