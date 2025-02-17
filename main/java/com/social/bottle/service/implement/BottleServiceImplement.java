package com.social.bottle.service.implement;

import com.social.bottle.dto.request.BottleLikeRequestDto;
import com.social.bottle.dto.request.CreateMessageRequestDto;
import com.social.bottle.dto.response.*;
import com.social.bottle.entity.Bottle;
import com.social.bottle.entity.BottleLike;
import com.social.bottle.entity.BottleMedia;
import com.social.bottle.repository.BottleLikeRepository;
import com.social.bottle.repository.BottleMediaRepository;
import com.social.bottle.repository.BottleRepository;
import com.social.bottle.service.BottleService;
import com.social.global.service.AwsS3Service;
import com.social.login.entity.User;
import com.social.login.provider.JWTProvider;
import com.social.login.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BottleServiceImplement implements BottleService {

    private final BottleRepository bottleRepository;
    private final BottleMediaRepository bottleMediaRepository;
    private final UserRepository userRepository;
    private final JWTProvider jwtProvider;
    private final AwsS3Service awsS3Service;
    private final BottleLikeRepository bottleLikeRepository;

    @Transactional
    @Override
    public CreateMessageResponseDto createMessage(String token, CreateMessageRequestDto request) {
        Integer userId = jwtProvider.validateToken(token);
        if (userId == null) {
            log.warn("❌ [유리병 작성 실패] 유효하지 않은 토큰");
            return CreateMessageResponseDto.unauthorized();
        }

        if (request.getTitle() == null || request.getTitle().isBlank() ||
                request.getContent() == null || request.getContent().isBlank()) {
            log.warn("❌ [유리병 작성 실패] 필수 필드 누락 (title, content)");
            return CreateMessageResponseDto.missingRequiredFields();
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.error("❌ [유리병 작성 실패] 사용자 정보 조회 실패");
            return CreateMessageResponseDto.serverError();
        }

        Bottle bottle = bottleRepository.save(Bottle.builder()
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .createdAt(Instant.now())
                .build());

        List<String> mediaUrls = new ArrayList<>();
        if (request.getMedia() != null) {
            for (MultipartFile file : request.getMedia()) {
                String mediaUrl = awsS3Service.uploadFile(file);
                bottleMediaRepository.save(BottleMedia.builder()
                        .bottle(bottle)
                        .mediaUrl(mediaUrl)
                        .build());
                mediaUrls.add(mediaUrl);
            }
        }

        log.info("✅ [유리병 작성 완료] bottleId={}, userId={}", bottle.getBottleId(), userId);
        return CreateMessageResponseDto.success(bottle.getBottleId(), bottle.getTitle(), bottle.getCreatedAt(), mediaUrls);
    }

    @Transactional
    @Override
    public RandomBottleResponseDto getRandomBottle(String token) {
        Integer userId = jwtProvider.validateToken(token);
        if (userId == null) {
            log.warn("❌ [유리병 랜덤 획득 실패] 인증 실패");
            return RandomBottleResponseDto.unauthorized();
        }

        Instant twentyFourHoursAgo = Instant.now().minusSeconds(24 * 60 * 60);

        List<Bottle> availableBottles = bottleRepository.findAvailableBottles(userId, twentyFourHoursAgo);

        if (availableBottles.isEmpty()) {
            log.info("📌 [유리병 랜덤 획득] 가능한 유리병 없음");
            return RandomBottleResponseDto.noBottleAvailable();
        }

        Random random = new Random();
        Bottle selectedBottle = availableBottles.get(random.nextInt(availableBottles.size()));

        log.info("✅ [유리병 랜덤 획득 성공] bottleId={}", selectedBottle.getBottleId());
        return RandomBottleResponseDto.success(selectedBottle.getBottleId());
    }

    @Override
    public BottleListResponseDto getExpiredBottles(String token) {
        Integer userId = jwtProvider.validateToken(token);
        if (userId == null) {
            log.warn("❌ [유리병 전체 조회 실패] 인증 실패");
            return BottleListResponseDto.unauthorized();
        }

        Instant twentyFourHoursAgo = Instant.now().minusSeconds(24 * 60 * 60);  // ✅ Instant 유지

        List<Bottle> expiredBottles = bottleRepository.findExpiredBottles(userId, twentyFourHoursAgo);

        if (expiredBottles.isEmpty()) {
            log.info("📌 [유리병 전체 조회] 24시간 지난 유리병 없음");
            return BottleListResponseDto.noBottlesAvailable();
        }

        log.info("✅ [유리병 전체 조회 성공] 유리병 개수={}", expiredBottles.size());

        return BottleListResponseDto.success(expiredBottles.stream()
                .map(b -> new BottleListResponseDto.BottleData(b.getBottleId(), b.getTitle()))
                .collect(Collectors.toList()));
    }

    @Override
    public BottleDetailResponseDto getBottleDetails(String token, Integer bottleId) {
        Integer userId = jwtProvider.validateToken(token);
        if (userId == null) {
            log.warn("❌ [유리병 상세 조회 실패] 인증 실패");
            return BottleDetailResponseDto.unauthorized();
        }

        if (bottleId == null) {
            log.warn("❌ [유리병 상세 조회 실패] bottle_id가 제공되지 않음");
            return BottleDetailResponseDto.missingBottleId();
        }

        Bottle bottle = bottleRepository.findById(bottleId)
                .orElse(null);

        if (bottle == null) {
            log.warn("❌ [유리병 상세 조회 실패] 존재하지 않는 유리병 ID={}", bottleId);
            return BottleDetailResponseDto.bottleNotFound();
        }

        List<String> mediaUrls = bottleMediaRepository.findByBottle_BottleId(bottleId)
                .stream().map(media -> media.getMediaUrl()).collect(Collectors.toList());

        int likeCount = bottleLikeRepository.countLikes(bottleId);

        log.info("✅ [유리병 상세 조회 성공] bottle_id={}, title={}", bottleId, bottle.getTitle());

        return BottleDetailResponseDto.success(
                bottle.getTitle(),
                bottle.getContent(),
                bottle.getCreatedAt(),
                mediaUrls,
                likeCount
        );
    }

    @Override
    @Transactional
    public BottleDeleteResponseDto deleteBottle(String token, Integer bottleId) {
        Integer userId = jwtProvider.validateToken(token);
        if (userId == null) {
            log.warn("❌ [유리병 삭제 실패] 인증 실패");
            return BottleDeleteResponseDto.unauthorized();
        }

        if (bottleId == null) {
            log.warn("❌ [유리병 삭제 실패] bottle_id가 제공되지 않음");
            return BottleDeleteResponseDto.missingBottleId();
        }

        Bottle bottle = bottleRepository.findByIdAndUserId(bottleId, userId).orElse(null);
        if (bottle == null) {
            log.warn("❌ [유리병 삭제 실패] 존재하지 않거나 본인 작성 아님 bottle_id={}", bottleId);
            return BottleDeleteResponseDto.forbidden();
        }

        // ✅ S3 파일 삭제
        List<BottleMedia> mediaList = bottleMediaRepository.findByBottle_BottleId(bottleId);
        for (BottleMedia media : mediaList) {
            awsS3Service.deleteFileFromS3(media.getMediaUrl());
        }

        // ✅ 최종적으로 유리병 삭제
        bottleRepository.delete(bottle);

        log.info("✅ [유리병 삭제 성공] bottle_id={}", bottleId);
        return BottleDeleteResponseDto.success();
    }

    @Override
    @Transactional
    public BottleLikeResponseDto likeBottle(String token, BottleLikeRequestDto requestDto) {
        Integer userId = jwtProvider.validateToken(token);
        if (userId == null) {
            log.warn("❌ [유리병 좋아요 실패] 인증 오류");
            return BottleLikeResponseDto.unauthorized();
        }

        if (requestDto.getBottleId() == null) {
            log.warn("❌ [유리병 좋아요 실패] bottle_id 누락");
            return BottleLikeResponseDto.missingBottleId();
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.error("❌ [유리병 좋아요 실패] 사용자 정보 조회 실패");
            return BottleLikeResponseDto.unauthorized();
        }

        Bottle bottle = bottleRepository.findById(requestDto.getBottleId()).orElse(null);
        if (bottle == null) {
            log.warn("❌ [유리병 좋아요 실패] 존재하지 않는 bottle_id={}", requestDto.getBottleId());
            return BottleLikeResponseDto.bottleNotFound();
        }

        if (bottleLikeRepository.existsByUserAndBottle(user, bottle)) {
            log.warn("❌ [유리병 좋아요 실패] 이미 좋아요를 누른 bottle_id={}", requestDto.getBottleId());
            return BottleLikeResponseDto.alreadyLiked();
        }

        BottleLike bottleLike = BottleLike.builder()
                .user(user)
                .bottle(bottle)
                .build();
        bottleLikeRepository.save(bottleLike);

        log.info("✅ [유리병 좋아요 성공] bottle_id={}, user_id={}", requestDto.getBottleId(), userId);
        return BottleLikeResponseDto.success();
    }

    @Override
    @Transactional
    public BottleLikeResponseDto removeLike(String token, BottleLikeRequestDto requestDto) {
        Integer userId = jwtProvider.validateToken(token);
        if (userId == null) {
            log.warn("❌ [유리병 좋아요 삭제 실패] 인증 오류");
            return BottleLikeResponseDto.unauthorized();
        }

        if (requestDto.getBottleId() == null) {
            log.warn("❌ [유리병 좋아요 삭제 실패] bottle_id 누락");
            return BottleLikeResponseDto.missingBottleId();
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.error("❌ [유리병 좋아요 삭제 실패] 사용자 정보 조회 실패");
            return BottleLikeResponseDto.unauthorized();
        }

        Bottle bottle = bottleRepository.findById(requestDto.getBottleId()).orElse(null);
        if (bottle == null) {
            log.warn("❌ [유리병 좋아요 삭제 실패] 존재하지 않는 bottle_id={}", requestDto.getBottleId());
            return BottleLikeResponseDto.bottleNotFound();
        }

        BottleLike bottleLike = bottleLikeRepository.findByUserAndBottle(user, bottle).orElse(null);
        if (bottleLike == null) {
            log.warn("❌ [유리병 좋아요 삭제 실패] 좋아요를 누르지 않은 bottle_id={}", requestDto.getBottleId());
            return BottleLikeResponseDto.notLiked();
        }

        bottleLikeRepository.delete(bottleLike);
        log.info("✅ [유리병 좋아요 삭제 성공] bottle_id={}, user_id={}", requestDto.getBottleId(), userId);
        return BottleLikeResponseDto.success();
    }

    @Override
    @Transactional(readOnly = true)
    public BottleLikeCountResponseDto getLikeCount(String token, Integer bottleId) {
        Integer userId = jwtProvider.validateToken(token);
        if (userId == null) {
            log.warn("❌ [유리병 좋아요 개수 조회 실패] 인증 오류");
            return BottleLikeCountResponseDto.unauthorized();
        }

        if (bottleId == null) {
            log.warn("❌ [유리병 좋아요 개수 조회 실패] bottle_id 누락");
            return BottleLikeCountResponseDto.missingBottleId();
        }

        Bottle bottle = bottleRepository.findById(bottleId).orElse(null);
        if (bottle == null) {
            log.warn("❌ [유리병 좋아요 개수 조회 실패] 존재하지 않는 bottle_id={}", bottleId);
            return BottleLikeCountResponseDto.bottleNotFound();
        }

        int likeCount = bottleLikeRepository.countByBottle(bottle);
        if (likeCount == 0) {
            log.info("✅ [유리병 좋아요 개수 조회] bottle_id={}, 좋아요 없음", bottleId);
            return BottleLikeCountResponseDto.noLikes(bottleId);
        }

        log.info("✅ [유리병 좋아요 개수 조회 성공] bottle_id={}, 좋아요 개수={}", bottleId, likeCount);
        return BottleLikeCountResponseDto.success(bottleId, likeCount);
    }

    @Transactional(readOnly = true)
    @Override
    public BottleLikeStatusResponseDto getLikeStatus(String token, Integer bottleId) {
        // 1. JWT 검증 및 사용자 추출
        Integer userId = jwtProvider.validateToken(token);
        if (userId == null) {
            log.warn("❌ [좋아요 상태 조회 실패] 인증 오류");
            return BottleLikeStatusResponseDto.authRequired();
        }

        // 2. 필수 파라미터 및 유리병 존재 체크
        if (bottleId == null) {
            log.warn("❌ [좋아요 상태 조회 실패] 필수 값 누락");
            return BottleLikeStatusResponseDto.missingBottleId();
        }
        if (!bottleRepository.existsById(bottleId)) {
            log.warn("❌ [좋아요 상태 조회 실패] 존재하지 않는 유리병 ID: {}", bottleId);
            return BottleLikeStatusResponseDto.bottleNotFound();
        }

        // 3. 좋아요 상태 확인
        boolean isLiked = bottleLikeRepository.existsByBottle_BottleIdAndUser_UserId(bottleId, userId);
        String status = isLiked ? "LIKED" : "NOT_LIKED";

        log.info("✅ [좋아요 상태 조회 완료] 사용자 ID: {}, 유리병 ID: {}, 상태: {}", userId, bottleId, status);
        return BottleLikeStatusResponseDto.success(bottleId, status);
    }
}
