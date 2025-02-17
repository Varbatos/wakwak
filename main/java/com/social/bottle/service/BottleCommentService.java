package com.social.bottle.service;

import com.social.bottle.dto.request.BottleCommentRequestDto;
import com.social.bottle.dto.request.DeleteBottleCommentRequestDto;
import com.social.bottle.dto.response.BottleCommentResponseDto;
import com.social.bottle.dto.response.DeleteBottleCommentResponseDto;
import com.social.bottle.dto.response.GetBottleCommentResponseDto;

import java.util.List;

public interface BottleCommentService {
    BottleCommentResponseDto addComment(String token, BottleCommentRequestDto requestDto);

    GetBottleCommentResponseDto getComments(String token, Integer bottleId);

    DeleteBottleCommentResponseDto deleteComment(String token, DeleteBottleCommentRequestDto requestDto);
}
