package com.social.friends.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendFriendRequestDto {
    private Integer userId; // JWT에서 추출한 사용자 ID
}
