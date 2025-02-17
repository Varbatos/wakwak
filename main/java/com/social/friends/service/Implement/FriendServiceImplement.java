package com.social.friends.service.Implement;

import com.social.friends.dto.FriendProjection;
import com.social.friends.dto.request.FriendAcceptRequestDto;
import com.social.friends.dto.request.FriendDeleteRequestDto;
import com.social.friends.dto.request.FriendRejectRequestDto;
import com.social.friends.dto.request.FriendRequestDto;
import com.social.friends.dto.response.*;
import com.social.friends.entity.Friend;
import com.social.friends.entity.FriendRequest;
import com.social.friends.repository.FriendRepository;
import com.social.friends.repository.FriendRequestRepository;
import com.social.friends.service.FriendService;
import com.social.login.entity.User;
import com.social.login.provider.JWTProvider;
import com.social.login.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FriendServiceImplement implements FriendService {

    private final FriendRepository friendRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final UserRepository userRepository;
    private final JWTProvider jwtProvider;

    @Override
    @Transactional
    public FriendRequestResponseDto sendFriendRequest(String token, FriendRequestDto request) {
        log.info("📌 [친구 요청 시작] 토큰 검증 및 senderId 추출");

        Integer senderId = jwtProvider.validateToken(token);
        if (senderId == null) {
            log.warn("❌ [인증 실패] 유효하지 않은 토큰");
            return FriendRequestResponseDto.unauthorized();
        }

        log.info("✅ [인증 성공] senderId: {}", senderId);

        if (request.getReceiverId() == null) {
            log.warn("❌ [친구 요청 실패] receiverId가 없음");
            return FriendRequestResponseDto.missingReceiverId();
        }

        if (senderId.equals(request.getReceiverId())) {
            log.warn("❌ [친구 요청 실패] 자기 자신에게 요청 불가 (senderId={}, receiverId={})", senderId, request.getReceiverId());
            return FriendRequestResponseDto.cannotAddSelf();
        }

        log.info("📌 [친구 요청 대상 확인] senderId: {}, receiverId: {}", senderId, request.getReceiverId());

        User sender = userRepository.findById(senderId).orElse(null);
        User receiver = userRepository.findById(request.getReceiverId()).orElse(null);

        if (sender == null || receiver == null) {
            log.error("❌ [친구 요청 실패] sender 또는 receiver가 존재하지 않음 (senderId={}, receiverId={})", senderId, request.getReceiverId());
            return FriendRequestResponseDto.serverError();
        }

        log.info("✅ [사용자 확인 완료] sender: {}, receiver: {}", sender.getUsername(), receiver.getUsername());

        if (friendRepository.existsByUser1UserIdAndUser2UserId(senderId, request.getReceiverId()) ||
                friendRepository.existsByUser1UserIdAndUser2UserId(request.getReceiverId(), senderId)) {
            log.warn("❌ [친구 요청 실패] 이미 친구 관계 (senderId={}, receiverId={})", senderId, request.getReceiverId());
            return FriendRequestResponseDto.alreadyFriends();
        }

        if (friendRequestRepository.existsBySenderUserIdAndReceiverUserId(senderId, request.getReceiverId()) ||
        friendRequestRepository.existsBySenderUserIdAndReceiverUserId(request.getReceiverId(), senderId)) {
            log.warn("❌ [친구 요청 실패] 이미 친구 요청이 존재함 (senderId={}, receiverId={})", senderId, request.getReceiverId());
            return FriendRequestResponseDto.requestAlreadyExists();
        }

        log.info("✅ [친구 요청 저장] senderId={}, receiverId={}", senderId, request.getReceiverId());

        friendRequestRepository.save(FriendRequest.builder()
                .sender(sender)
                .receiver(receiver)
                .build());

        log.info("✅ [친구 요청 완료] senderId={}, receiverId={}", senderId, request.getReceiverId());
        return FriendRequestResponseDto.success(senderId, request.getReceiverId());
    }

    @Override
    @Transactional(readOnly = true)
    public FriendRequestListResponseDto getFriendRequests(String token) {
        log.info("📌 [친구 요청 조회] Authorization 토큰 수신");

        // ✅ 토큰 검증 및 userId 추출
        Integer receiverId = jwtProvider.validateToken(token);
        if (receiverId == null) {
            log.warn("❌ [친구 요청 조회 실패] 인증 오류 (토큰 검증 실패)");
            return FriendRequestListResponseDto.unauthorized();
        }

        // ✅ userId로 사용자 조회
        User receiver = userRepository.findById(receiverId).orElse(null);
        if (receiver == null) {
            log.error("❌ [친구 요청 조회 실패] 유효하지 않은 사용자 ID: {}", receiverId);
            return FriendRequestListResponseDto.invalidToken();
        }

        log.info("✅ [토큰 인증 성공] userId={}", receiverId);

        // ✅ 친구 요청 목록 조회
        List<FriendRequest> friendRequests = friendRequestRepository.findByReceiverId(receiverId);
        if (friendRequests.isEmpty()) {
            log.info("✅ [친구 요청 없음] userId={}", receiverId);
            return FriendRequestListResponseDto.noRequests();
        }

        // ✅ 응답 변환
        List<FriendRequestListResponseDto.Data> requestList = friendRequests.stream()
                .map(fr -> new FriendRequestListResponseDto.Data(
                        fr.getSender().getUserId(),
                        fr.getSender().getNickname(),
                        fr.getSender().getMediaUrl() // UserEntity에 profileImage 필드 필요
                ))
                .collect(Collectors.toList());

        log.info("✅ [친구 요청 조회 성공] userId={}, 요청 개수={}", receiverId, requestList.size());
        return FriendRequestListResponseDto.success(requestList);
    }

    @Override
    @Transactional
    public FriendAcceptResponseDto acceptFriendRequest(String token, FriendAcceptRequestDto request) {
        log.info("📌 [친구 요청 수락] Authorization 토큰 수신");

        // ✅ 토큰 검증 및 userId 추출
        Integer receiverId = jwtProvider.validateToken(token);
        if (receiverId == null) {
            log.warn("❌ [친구 요청 수락 실패] 인증 오류 (토큰 검증 실패)");
            return FriendAcceptResponseDto.unauthorized();
        }

        if (request.getSenderId() == null) {
            log.warn("❌ [친구 요청 수락 실패] sender_id 누락");
            return FriendAcceptResponseDto.missingSenderId();
        }

        // ✅ userId로 사용자 조회
        User receiver = userRepository.findById(receiverId).orElse(null);
        User sender = userRepository.findById(request.getSenderId()).orElse(null);
        if (receiver == null || sender == null) {
            log.error("❌ [친구 요청 수락 실패] 유효하지 않은 senderId={}, receiverId={}", request.getSenderId(), receiverId);
            return FriendAcceptResponseDto.invalidToken();
        }

        log.info("✅ [토큰 인증 성공] receiverId={}", receiverId);

        // ✅ 이미 친구인지 확인
        if (friendRepository.existsByUsers(sender, receiver) ||
                friendRepository.existsByUsers(receiver, sender)) {
            log.warn("❌ [친구 요청 수락 실패] 이미 친구 상태: senderId={}, receiverId={}", request.getSenderId(), receiverId);
            return FriendAcceptResponseDto.alreadyFriends();
        }

        // ✅ 친구 요청 존재 여부 확인
        if (!friendRequestRepository.existsBySenderAndReceiver(sender, receiver)) {
            log.warn("❌ [친구 요청 수락 실패] 요청이 존재하지 않음: senderId={}, receiverId={}", request.getSenderId(), receiverId);
            return FriendAcceptResponseDto.requestNotFound();
        }

        // ✅ 친구 요청 삭제
        friendRequestRepository.deleteBySenderAndReceiver(sender, receiver);
        friendRequestRepository.deleteBySenderAndReceiver(receiver, sender);

        // ✅ 친구 관계 저장
        friendRepository.save(Friend.builder().user1(sender).user2(receiver).build());

        log.info("✅ [친구 요청 수락 성공] senderId={}, receiverId={}", request.getSenderId(), receiverId);
        return FriendAcceptResponseDto.success(request.getSenderId(), receiverId);
    }

    @Override
    @Transactional
    public FriendRejectResponseDto rejectFriendRequest(String token, FriendRejectRequestDto request) {
        log.info("📌 [친구 요청 거절] Authorization 토큰 수신");

        // ✅ 토큰 검증 및 userId 추출
        Integer receiverId = jwtProvider.validateToken(token);
        if (receiverId == null) {
            log.warn("❌ [친구 요청 거절 실패] 인증 오류 (토큰 검증 실패)");
            return FriendRejectResponseDto.unauthorized();
        }

        if (request.getSenderId() == null) {
            log.warn("❌ [친구 요청 거절 실패] sender_id 누락");
            return FriendRejectResponseDto.missingSenderId();
        }

        // ✅ userId로 사용자 조회
        User receiver = userRepository.findById(receiverId).orElse(null);
        User sender = userRepository.findById(request.getSenderId()).orElse(null);
        if (receiver == null || sender == null) {
            log.error("❌ [친구 요청 거절 실패] 유효하지 않은 senderId={}, receiverId={}", request.getSenderId(), receiverId);
            return FriendRejectResponseDto.invalidToken();
        }

        log.info("✅ [토큰 인증 성공] receiverId={}", receiverId);

        // ✅ 이미 친구인지 확인
        if (friendRepository.existsByUsers(sender, receiver) ||
                friendRepository.existsByUsers(receiver, sender)) {
            log.warn("❌ [친구 요청 수락 실패] 이미 친구 상태: senderId={}, receiverId={}", request.getSenderId(), receiverId);
            return FriendRejectResponseDto.alreadyFriends();
        }

        // ✅ 친구 요청 존재 여부 확인
        if (!friendRequestRepository.existsBySenderAndReceiver(sender, receiver)) {
            log.warn("❌ [친구 요청 수락 실패] 요청이 존재하지 않음: senderId={}, receiverId={}", request.getSenderId(), receiverId);
            return FriendRejectResponseDto.requestNotFound();
        }

        // ✅ 친구 요청 삭제
        friendRequestRepository.deleteBySenderAndReceiver(sender, receiver);
        friendRequestRepository.deleteBySenderAndReceiver(receiver, sender);

        log.info("✅ [친구 요청 거절 성공] senderId={}, receiverId={}", request.getSenderId(), receiverId);
        return FriendRejectResponseDto.success(request.getSenderId(), receiverId);
    }

    @Transactional(readOnly = true)
    public GetFriendsResponseDto getFriends(String token) {
        Integer userId = jwtProvider.validateToken(token);
        if (userId == null) {
            log.warn("❌ [친구 목록 조회 실패] 유효하지 않은 토큰");
            return GetFriendsResponseDto.unauthorized();
        }

        log.info("✅ [친구 목록 조회 요청] userId={}", userId);
        List<FriendProjection> friends = friendRepository.findFriendsByUserId(userId);

        if (friends.isEmpty()) {
            log.info("✅ [친구 없음] userId={}", userId);
            return GetFriendsResponseDto.noFriends();
        }

        return GetFriendsResponseDto.success(friends);
    }

    @Override
    @Transactional
    public FriendDeleteResponseDto deleteFriend(String token, FriendDeleteRequestDto request) {
        Integer userId = jwtProvider.validateToken(token);
        if (userId == null) return FriendDeleteResponseDto.unauthorized();

        Integer friendId = request.getFriendId();
        if (friendId == null) return FriendDeleteResponseDto.missingFriendId();
        if (userId.equals(friendId)) return FriendDeleteResponseDto.cannotDeleteSelf();

        // ✅ 친구 관계 존재 여부 확인
        if (!friendRepository.existsByUser1UserIdAndUser2UserId(userId, friendId) &&
                !friendRepository.existsByUser1UserIdAndUser2UserId(friendId, userId)) {
            return FriendDeleteResponseDto.friendNotFound();
        }

        // ✅ 친구 삭제
        int deleted = friendRepository.deleteFriendship(userId, friendId);
        if (deleted > 0) {
            log.info("✅ [친구 삭제 성공] userId={}, friendId={}", userId, friendId);
            return FriendDeleteResponseDto.success(userId, friendId);
        } else {
            log.warn("❌ [친구 삭제 실패] 존재하지 않는 친구 관계 - userId={}, friendId={}", userId, friendId);
            return FriendDeleteResponseDto.friendNotFound();
        }
    }

    @Override
    public FriendSearchResponseDto searchFriends(String token, String nickname) {
        Integer userId = jwtProvider.validateToken(token);
        if (userId == null) return FriendSearchResponseDto.unauthorized();

        if (nickname == null || nickname.trim().isEmpty()) return FriendSearchResponseDto.missingNickname();

        // ✅ 닉네임 검색 실행
        List<User> users = userRepository.findByNicknameContaining(nickname);
        if (users.isEmpty()) return FriendSearchResponseDto.noResults();

        List<FriendSearchResponseDto.Data> results = users.stream()
                .filter(user -> !user.getUserId().equals(userId))
                .map(user -> new FriendSearchResponseDto.Data(user.getUserId(), user.getNickname(), user.getMediaUrl()))
                .collect(Collectors.toList());

        log.info("✅ [친구 검색 성공] 검색어: {}, 검색 결과 수: {}", nickname, results.size());
        return FriendSearchResponseDto.success(results);
    }

    @Override
    @Transactional(readOnly = true)
    public SearchFriendStatusResponseDto getFriendStatus(String token, Integer targetId) {
        Integer userId = jwtProvider.validateToken(token);
        if (userId == null) return SearchFriendStatusResponseDto.unauthorized();

        if (targetId == null) return SearchFriendStatusResponseDto.missingTargetId();
        if (userId.equals(targetId)) return SearchFriendStatusResponseDto.cannotCheckSelf();

        log.info("🔍 [친구 상태 조회] 요청: userId={}, targetId={}", userId, targetId);

        if (friendRequestRepository.existsSentRequest(userId, targetId)) {
            log.info("✅ [친구 상태] PENDING (친구 요청을 보낸 상태)");
            return SearchFriendStatusResponseDto.success(userId, targetId, "PENDING");
        }

        if (friendRepository.existsByFriendship(userId, targetId)) {
            log.info("✅ [친구 상태] FRIENDS (이미 친구)");
            return SearchFriendStatusResponseDto.success(userId, targetId, "FRIENDS");
        }

        if (friendRequestRepository.existsReceivedRequest(userId, targetId)) {
            log.info("✅ [친구 상태] RECEIVED (상대가 나에게 친구 요청을 보낸 상태)");
            return SearchFriendStatusResponseDto.success(userId, targetId, "RECEIVED");
        }

        log.info("✅ [친구 상태] NOT_FRIENDS (아무 관계 아님)");
        return SearchFriendStatusResponseDto.success(userId, targetId, "NOT_FRIENDS");
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendFriendResponseDto recommendFriends(String token) {
        Integer userId = jwtProvider.validateToken(token);
        if (userId == null) return RecommendFriendResponseDto.unauthorized();

        log.info("🔍 [친구 추천 조회] userId={}", userId);

        // ✅ 1단계 친구 조회
        List<Integer> firstDegreeFriends = friendRepository.findFirstDegreeFriends(userId);
        log.info("✅ [1단계 친구 조회 완료] friends={}", firstDegreeFriends);

        // ✅ 2단계 친구 조회
        Map<Integer, Integer> secondDegreeFriendCount = new HashMap<>();
        for (Integer friendId : firstDegreeFriends) {
            List<Integer> secondDegreeFriends = friendRepository.findSecondDegreeFriends(friendId, userId);
            for (Integer secondFriend : secondDegreeFriends) {
                if (!firstDegreeFriends.contains(secondFriend) && !secondFriend.equals(userId)) {
                    secondDegreeFriendCount.put(secondFriend, secondDegreeFriendCount.getOrDefault(secondFriend, 0) + 1);
                }
            }
        }

        // ✅ 연결 개수가 많은 순으로 정렬
        List<Integer> recommendedFriendIds = secondDegreeFriendCount.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // ✅ 추천된 친구들의 정보 가져오기
        List<User> recommendedUsers = userRepository.findUsersByIds(recommendedFriendIds);
        log.info("✅ [추천 친구 정보 조회 완료] recommendedUsers={}", recommendedUsers);

        // ✅ Response 데이터 생성
        List<RecommendFriendResponseDto.Data> responseData = recommendedUsers.stream()
                .map(user -> new RecommendFriendResponseDto.Data(
                        user.getUserId(),
                        user.getNickname(),
                        user.getMediaUrl(),
                        secondDegreeFriendCount.get(user.getUserId())
                ))
                .collect(Collectors.toList());

        return RecommendFriendResponseDto.success(responseData);
    }

    @Override
    public BluetoothSearchResponseDto searchNearbyUsers(String token, List<String> deviceIds) {
        // 1️⃣ 토큰 검증
        Integer userId = jwtProvider.validateToken(token);
        if (userId == null) {
            log.warn("❌ [블루투스 사용자 검색 실패] 인증 오류");
            return BluetoothSearchResponseDto.authRequired();
        }

        // 2️⃣ 필수 필드 체크
        if (deviceIds == null || deviceIds.isEmpty()) {
            log.warn("❌ [블루투스 사용자 검색 실패] device_ids 누락");
            return BluetoothSearchResponseDto.missingDeviceIds();
        }

        // 3️⃣ 블루투스로 감지된 기기 ID와 매칭되는 유저 조회
        List<User> detectedUsers = userRepository.findByDeviceIdIn(deviceIds);
        log.info("✅ [블루투스 검색] 감지된 사용자 수: {}", detectedUsers.size());

        // 4️⃣ DTO 변환 후 반환
        List<BluetoothSearchResponseDto.Data> result = detectedUsers.stream()
                .filter(user -> !user.getUserId().equals(userId))
                .map(user -> new BluetoothSearchResponseDto.Data(user.getUserId(), user.getNickname(), user.getMediaUrl()))
                .collect(Collectors.toList());

        if (result.isEmpty()) {
            log.info("✅ [블루투스 검색] 감지된 사용자 없음");
            return BluetoothSearchResponseDto.noNearbyUsers();
        }

        log.info("✅ [블루투스 검색 완료] 검색된 사용자 수: {}", result.size());
        return BluetoothSearchResponseDto.success(result);
    }

}
