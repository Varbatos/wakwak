package com.social.bottle.controller;

import com.social.bottle.dto.request.BottleDeleteRequestDto;
import com.social.bottle.dto.request.BottleLikeRequestDto;
import com.social.bottle.dto.request.CreateMessageRequestDto;
import com.social.bottle.dto.response.*;
import com.social.bottle.service.BottleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/bottle")
@RequiredArgsConstructor
@Slf4j
public class BottleController {

    private final BottleService bottleService;

    @PostMapping
    public ResponseEntity<CreateMessageResponseDto> createMessage(
            @RequestHeader("Authorization") String authorization,
            @RequestPart("title") String title,
            @RequestPart("content") String content,
            @RequestPart(value = "media", required = false) List<MultipartFile> media) {

        log.info("📌 [유리병 작성 API 호출] Authorization 헤더 수신");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("❌ [유리병 작성 실패] Authorization 헤더 없음 또는 형식 오류");
            return ResponseEntity.status(401).body(CreateMessageResponseDto.unauthorized());
        }

        String token = authorization.replace("Bearer ", "");
        log.info("✅ [토큰 추출 완료] token={}", token);

        CreateMessageRequestDto requestDto = new CreateMessageRequestDto();
        requestDto.setTitle(title);
        requestDto.setContent(content);
        requestDto.setMedia(media);

        log.info("📌 [유리병 작성 시작] 제목={}, 내용 길이={}, 첨부파일 수={}",
                requestDto.getTitle(), requestDto.getContent().length(),
                (requestDto.getMedia() != null) ? requestDto.getMedia().size() : 0);

        CreateMessageResponseDto response = bottleService.createMessage(token, requestDto);

        if ("SUCCESS".equals(response.getCode())) {
            log.info("✅ [유리병 작성 성공] bottleId={}, title={}",
                    response.getData().getBottleId(), response.getData().getTitle());
            return ResponseEntity.status(201).body(response);
        } else {
            log.warn("❌ [유리병 작성 실패] 이유: {}", response.getMessage());
            return ResponseEntity.status(400).body(response);
        }
    }

    @GetMapping("/random")
    public ResponseEntity<RandomBottleResponseDto> getRandomBottle(
            @RequestHeader("Authorization") String authorization) {

        log.info("📌 [랜덤 유리병 조회 API 호출] Authorization 헤더 수신");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("❌ [랜덤 유리병 조회 실패] Authorization 헤더 없음 또는 형식 오류");
            return ResponseEntity.status(401).body(RandomBottleResponseDto.unauthorized());
        }

        String token = authorization.replace("Bearer ", "");
        log.info("✅ [토큰 추출 완료] token={}", token);

        RandomBottleResponseDto response = bottleService.getRandomBottle(token);

        if ("SUCCESS".equals(response.getCode())) {
            log.info("✅ [랜덤 유리병 조회 성공] bottleId={}", response.getData().getBottleId());
            return ResponseEntity.ok(response);
        } else {
            log.warn("❌ [랜덤 유리병 조회 실패] 이유: {}", response.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<BottleListResponseDto> getExpiredBottles(
            @RequestHeader("Authorization") String authorization) {

        log.info("📌 [유리병 전체 조회 API 호출] Authorization 헤더 수신");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("❌ [유리병 전체 조회 실패] Authorization 헤더 없음 또는 형식 오류");
            return ResponseEntity.status(401).body(BottleListResponseDto.unauthorized());
        }

        String token = authorization.replace("Bearer ", "");
        log.info("✅ [토큰 추출 완료] token={}", token);

        BottleListResponseDto response = bottleService.getExpiredBottles(token);

        if ("SUCCESS".equals(response.getCode())) {
            log.info("✅ [유리병 전체 조회 성공] 유리병 개수={}", response.getData().size());
            return ResponseEntity.status(200).body(response);
        } else {
            log.warn("❌ [유리병 전체 조회 실패] 이유: {}", response.getMessage());
            return ResponseEntity.status(response.getHttpStatus()).body(response);
        }
    }

    @GetMapping("/detail")
    public ResponseEntity<BottleDetailResponseDto> getBottleDetails(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("bottleId") Integer bottleId) {

        log.info("📌 [유리병 상세 조회 API 호출] bottle_id={}", bottleId);

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("❌ [유리병 상세 조회 실패] Authorization 헤더 없음 또는 형식 오류");
            return ResponseEntity.status(401).body(BottleDetailResponseDto.unauthorized());
        }

        String token = authorization.replace("Bearer ", "");
        log.info("✅ [토큰 추출 완료] token={}", token);

        BottleDetailResponseDto response = bottleService.getBottleDetails(token, bottleId);

        if ("SUCCESS".equals(response.getCode())) {
            log.info("✅ [유리병 상세 조회 성공] bottle_id={}", bottleId);
            return ResponseEntity.status(200).body(response);
        } else {
            log.warn("❌ [유리병 상세 조회 실패] 이유: {}", response.getMessage());
            return ResponseEntity.status(response.getHttpStatus()).body(response);
        }
    }

    @PostMapping("/delete")
    public ResponseEntity<BottleDeleteResponseDto> deleteBottle(
            @RequestHeader("Authorization") String authorization,
            @RequestBody BottleDeleteRequestDto request) {

        log.info("📌 [유리병 삭제 API 호출] bottle_id={}", request.getBottleId());

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("❌ [유리병 삭제 실패] Authorization 헤더 없음 또는 형식 오류");
            return ResponseEntity.status(401).body(BottleDeleteResponseDto.unauthorized());
        }

        String token = authorization.replace("Bearer ", "");
        log.info("✅ [토큰 추출 완료] token={}", token);

        BottleDeleteResponseDto response = bottleService.deleteBottle(token, request.getBottleId());

        if ("SUCCESS".equals(response.getCode())) {
            log.info("✅ [유리병 삭제 성공] bottle_id={}", request.getBottleId());
            return ResponseEntity.status(200).body(response);
        } else {
            log.warn("❌ [유리병 삭제 실패] 이유: {}", response.getMessage());
            return ResponseEntity.status(response.getHttpStatus()).body(response);
        }
    }

    @PostMapping("/like")
    public ResponseEntity<BottleLikeResponseDto> likeBottle(
            @RequestHeader("Authorization") String authorization,
            @RequestBody BottleLikeRequestDto requestDto) {

        log.info("📌 [유리병 좋아요 API 호출] bottle_id={}", requestDto.getBottleId());

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("❌ [유리병 좋아요 실패] Authorization 헤더 없음 또는 형식 오류");
            return ResponseEntity.status(401).body(BottleLikeResponseDto.unauthorized());
        }

        String token = authorization.replace("Bearer ", "");
        log.info("✅ [토큰 추출 완료] token={}", token);

        BottleLikeResponseDto response = bottleService.likeBottle(token, requestDto);
        return ResponseEntity.status(response.getHttpStatus()).body(response);
    }

    @PostMapping("/like/delete")
    public ResponseEntity<BottleLikeResponseDto> removeLike(
            @RequestHeader("Authorization") String authorization,
            @RequestBody BottleLikeRequestDto requestDto) {

        log.info("📌 [유리병 좋아요 삭제 API 호출] bottle_id={}", requestDto.getBottleId());

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("❌ [유리병 좋아요 삭제 실패] Authorization 헤더 없음 또는 형식 오류");
            return ResponseEntity.status(401).body(BottleLikeResponseDto.unauthorized());
        }

        String token = authorization.replace("Bearer ", "");
        log.info("✅ [토큰 추출 완료] token={}", token);

        BottleLikeResponseDto response = bottleService.removeLike(token, requestDto);
        return ResponseEntity.status(response.getHttpStatus()).body(response);
    }

    @GetMapping("/like/count")
    public ResponseEntity<BottleLikeCountResponseDto> getLikeCount(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("bottleId") Integer bottleId) {

        log.info("📌 [유리병 좋아요 개수 조회 API 호출] bottle_id={}", bottleId);

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("❌ [유리병 좋아요 개수 조회 실패] Authorization 헤더 없음 또는 형식 오류");
            return ResponseEntity.status(401).body(BottleLikeCountResponseDto.unauthorized());
        }

        String token = authorization.replace("Bearer ", "");
        log.info("✅ [토큰 추출 완료] token={}", token);

        BottleLikeCountResponseDto response = bottleService.getLikeCount(token, bottleId);
        return ResponseEntity.status(response.getHttpStatus()).body(response);
    }

    @GetMapping("/like/status")
    public ResponseEntity<BottleLikeStatusResponseDto> getLikeStatus(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("bottle_id") Integer bottleId) {

        log.info("📌 [좋아요 상태 조회 요청] 유리병 ID: {}", bottleId);

        String token = authorization.replace("Bearer ", "");
        log.info("✅ [토큰 추출 완료] token={}", token);

        BottleLikeStatusResponseDto response = bottleService.getLikeStatus(token, bottleId);

        log.info("✅ [좋아요 상태 조회 완료] 유리병 ID: {}, 상태: {}", response.getData().getBottleId(), response.getData().getStatus());

        return ResponseEntity.ok(response);
    }
}
