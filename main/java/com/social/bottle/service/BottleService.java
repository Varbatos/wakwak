package com.social.bottle.service;

import com.social.bottle.dto.request.BottleCommentRequestDto;
import com.social.bottle.dto.request.BottleLikeRequestDto;
import com.social.bottle.dto.request.CreateMessageRequestDto;
import com.social.bottle.dto.response.*;

import java.util.List;

public interface BottleService {
    CreateMessageResponseDto createMessage(String token, CreateMessageRequestDto request);

    RandomBottleResponseDto getRandomBottle(String token);

    BottleListResponseDto getExpiredBottles(String token);

    BottleDetailResponseDto getBottleDetails(String token, Integer bottleId);

    BottleDeleteResponseDto deleteBottle(String token, Integer bottleId);

    BottleLikeResponseDto likeBottle(String token, BottleLikeRequestDto requestDto);

    BottleLikeResponseDto removeLike(String token, BottleLikeRequestDto requestDto);

    BottleLikeCountResponseDto getLikeCount(String token, Integer bottleId);

    BottleLikeStatusResponseDto getLikeStatus(String token, Integer bottleId);
}
