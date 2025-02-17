package com.social.friends.controller;

import com.social.friends.dto.request.FriendAcceptRequestDto;
import com.social.friends.dto.request.FriendDeleteRequestDto;
import com.social.friends.dto.request.FriendRejectRequestDto;
import com.social.friends.dto.request.FriendRequestDto;
import com.social.friends.dto.response.*;
import com.social.friends.service.FriendService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
@Slf4j
public class FriendController {

    private final FriendService friendService;

    @PostMapping("/send")
    public ResponseEntity<FriendRequestResponseDto> sendFriendRequest(
            @RequestHeader("Authorization") String authorization,
            @RequestBody FriendRequestDto request) {

        log.info("📌 [친구 요청 API 호출] Authorization 헤더 수신");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("❌ [친구 요청 실패] Authorization 헤더가 없음 또는 형식 오류");
            return ResponseEntity.status(401).body(FriendRequestResponseDto.unauthorized());
        }

        String token = authorization.replace("Bearer ", "");
        log.info("✅ [토큰 추출 완료] token={}", token);

        log.info("📌 [친구 요청 시작] 요청 데이터: receiverId={}", request.getReceiverId());

        FriendRequestResponseDto response = friendService.sendFriendRequest(token, request);

        if ("SUCCESS".equals(response.getCode())) {
            log.info("✅ [친구 요청 성공] senderId={}, receiverId={}", response.getData().getSenderId(), response.getData().getReceiverId());
        } else {
            log.warn("❌ [친구 요청 실패] 이유: {}", response.getMessage());
        }

        return ResponseEntity.status(response.getHttpStatus()).body(response); // ✅ 수정된 부분
    }

    @GetMapping("/requests")
    public ResponseEntity<FriendRequestListResponseDto> getFriendRequests(
            @RequestHeader("Authorization") String authorization) {

        log.info("📌 [GET /friends/requests] 친구 요청 조회 API 호출");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("❌ [친구 요청 조회 실패] Authorization 헤더가 없음 또는 형식 오류");
            return ResponseEntity.status(401).body(FriendRequestListResponseDto.unauthorized());
        }

        String token = authorization.replace("Bearer ", "");
        log.info("✅ [토큰 추출 완료] token={}", token);

        FriendRequestListResponseDto response = friendService.getFriendRequests(token);

        log.info("✅ [친구 요청 조회 완료] code={}, message={}", response.getCode(), response.getMessage());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/requests/accept")
    public ResponseEntity<FriendAcceptResponseDto> acceptFriendRequest(
            @RequestHeader("Authorization") String authorization,
            @RequestBody FriendAcceptRequestDto request) {
        String token = authorization.replace("Bearer ", "");
        FriendAcceptResponseDto response = friendService.acceptFriendRequest(token, request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @PostMapping("/requests/reject")
    public ResponseEntity<FriendRejectResponseDto> rejectFriendRequest(
            @RequestHeader("Authorization") String authorization,
            @RequestBody FriendRejectRequestDto request) {
        String token = authorization.replace("Bearer ", "");
        FriendRejectResponseDto response = friendService.rejectFriendRequest(token, request);
        return ResponseEntity.status(response.getStatus()).body(response);
    }

    @GetMapping
    public ResponseEntity<GetFriendsResponseDto> getFriends(
            @RequestHeader("Authorization") String authorization) {

        log.info("📌 [친구 목록 조회 API 호출] Authorization 헤더 수신");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("❌ [친구 목록 조회 실패] Authorization 헤더가 없음 또는 형식 오류");
            return ResponseEntity.status(401).body(GetFriendsResponseDto.unauthorized());
        }

        String token = authorization.replace("Bearer ", "");
        log.info("✅ [토큰 추출 완료] token={}", token);

        GetFriendsResponseDto response = friendService.getFriends(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/delete")
    public ResponseEntity<FriendDeleteResponseDto> deleteFriend(
            @RequestHeader("Authorization") String authorization,
            @RequestBody FriendDeleteRequestDto request) {

        log.info("📌 [친구 삭제 API 호출] Authorization 헤더 수신");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("❌ [친구 삭제 실패] Authorization 헤더 없음 또는 형식 오류");
            return ResponseEntity.status(401).body(FriendDeleteResponseDto.unauthorized());
        }

        String token = authorization.replace("Bearer ", "");
        log.info("✅ [토큰 추출 완료] token={}", token);

        log.info("📌 [친구 삭제 요청] friendId={}", request.getFriendId());

        FriendDeleteResponseDto response = friendService.deleteFriend(token, request);

        if ("SUCCESS".equals(response.getCode())) {
            log.info("✅ [친구 삭제 성공] userId={}, deletedFriendId={}",
                    response.getData().getUserId(), response.getData().getDeletedFriendId());
            return ResponseEntity.ok(response);
        } else {
            log.warn("❌ [친구 삭제 실패] 이유: {}", response.getMessage());
            return ResponseEntity.status(400).body(response);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<FriendSearchResponseDto> searchFriends(
            @RequestHeader("Authorization") String authorization,
            @RequestParam String nickname) {

        log.info("📌 [친구 검색 API 호출] Authorization 헤더 수신, 검색어: {}", nickname);

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("❌ [친구 검색 실패] Authorization 헤더 없음 또는 형식 오류");
            return ResponseEntity.status(401).body(FriendSearchResponseDto.unauthorized());
        }

        String token = authorization.replace("Bearer ", "");
        log.info("✅ [토큰 추출 완료] token={}", token);

        FriendSearchResponseDto response = friendService.searchFriends(token, nickname);

        if ("SUCCESS".equals(response.getCode())) {
            log.info("✅ [친구 검색 성공] 검색어: {}, 결과 수: {}", nickname, response.getData().size());
            return ResponseEntity.ok(response);
        } else {
            log.warn("❌ [친구 검색 실패] 이유: {}", response.getMessage());
            return ResponseEntity.status(400).body(response);
        }
    }
    @GetMapping("/search/status")
    public ResponseEntity<SearchFriendStatusResponseDto> getFriendStatus(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("target_id") Integer targetId) {

        log.info("📌 [친구 상태 조회 API 호출] targetId={}", targetId);

        String token = authorization.replace("Bearer ", "");
        SearchFriendStatusResponseDto response = friendService.getFriendStatus(token, targetId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/recommend-friend")
    public ResponseEntity<RecommendFriendResponseDto> recommendFriends(
            @RequestHeader("Authorization") String authorization) {

        log.info("📌 [추천 친구 조회 API 호출]");

        String token = authorization.replace("Bearer ", "");
        RecommendFriendResponseDto response = friendService.recommendFriends(token);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/search/bluetooth")
    public ResponseEntity<BluetoothSearchResponseDto> searchUsersByBluetooth(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("device_ids") List<String> deviceIds) {

        log.info("📌 [블루투스 사용자 검색 요청] device_ids: {}", deviceIds);

        String token = authorization.replace("Bearer ", "");
        BluetoothSearchResponseDto response = friendService.searchNearbyUsers(token, deviceIds);

        return ResponseEntity.status(response.getHttpStatus()).body(response);
    }

}
