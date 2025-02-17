package com.social.bottle.controller;

import com.social.bottle.dto.request.BottleCommentRequestDto;
import com.social.bottle.dto.request.DeleteBottleCommentRequestDto;
import com.social.bottle.dto.response.BottleCommentResponseDto;
import com.social.bottle.dto.response.DeleteBottleCommentResponseDto;
import com.social.bottle.dto.response.GetBottleCommentResponseDto;
import com.social.bottle.service.BottleCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bottle/comments")
@RequiredArgsConstructor
@Slf4j
public class BottleCommentController {

    private final BottleCommentService bottleCommentService;

    /**
     * ✅ 유리병 댓글 및 대댓글 작성 API
     */
    @PostMapping
    public ResponseEntity<BottleCommentResponseDto> addComment(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody BottleCommentRequestDto requestDto) {

        log.info("📌 [댓글 작성 요청] bottle_id={}, parent_id={}, content={}",
                requestDto.getBottleId(), requestDto.getParentId(), requestDto.getContent());
        String token = authorization.replace("Bearer ", "");
        log.info("✅ [토큰 추출 완료] token={}", token);

        BottleCommentResponseDto response = bottleCommentService.addComment(token, requestDto);

        log.info("📌 [댓글 작성 결과] status={}, message={}",
                response.getHttpStatus(), response.getMessage());

        return ResponseEntity.status(response.getHttpStatus()).body(response);
    }

    @GetMapping
    public ResponseEntity<GetBottleCommentResponseDto> getComments(
            @RequestHeader("Authorization") String authorization,
            @RequestParam Integer bottleId) {

        String token = authorization.replace("Bearer ", "");
        log.info("✅ [토큰 추출 완료] token={}", token);
        GetBottleCommentResponseDto response = bottleCommentService.getComments(token, bottleId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/delete")
    public ResponseEntity<DeleteBottleCommentResponseDto> deleteComment(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody DeleteBottleCommentRequestDto requestDto) {
        log.info("🗑️ Received delete request for comment: {}", requestDto.getCommentId());

        String token = authorization.replace("Bearer ", "");
        log.info("✅ [토큰 추출 완료] token={}", token);

        DeleteBottleCommentResponseDto response = bottleCommentService.deleteComment(token, requestDto);
        return ResponseEntity.status(response.getHttpStatus()).body(response);
    }
}
