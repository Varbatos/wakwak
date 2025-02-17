package com.social.timecapsules.service;

import com.social.global.service.AwsS3Service;
import com.social.login.entity.User;
import com.social.login.provider.JWTProvider;
import com.social.login.repository.UserRepository;
import com.social.timecapsules.dto.request.CreateTimeCapsuleRequestDto;
import com.social.timecapsules.dto.request.GetTimeCapsuleMapRequestDto;
import com.social.timecapsules.dto.request.TimeCapsuleDeleteRequestDto;
import com.social.timecapsules.dto.response.*;
import com.social.timecapsules.entity.TimeCapsule;
import com.social.timecapsules.entity.TimeCapsuleAccessUser;
import com.social.timecapsules.entity.TimeCapsuleMedia;
import com.social.timecapsules.repository.TimeCapsuleAccessUserRepository;
import com.social.timecapsules.repository.TimeCapsuleMediaRepository;
import com.social.timecapsules.repository.TimeCapsuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TimeCapsuleServiceImplement implements TimeCapsuleService {

    private final TimeCapsuleRepository timeCapsuleRepository;
    private final TimeCapsuleMediaRepository timeCapsuleMediaRepository;
    private final TimeCapsuleAccessUserRepository timeCapsuleAccessUserRepository;
    private final UserRepository userRepository;
    private final JWTProvider jwtProvider;
    private final AwsS3Service awsS3Service;

    @Transactional
    public CreateTimeCapsuleResponseDto createTimeCapsule(String token, CreateTimeCapsuleRequestDto request, List<MultipartFile> files) {
        Integer userId = jwtProvider.validateToken(token);
        if (userId == null) {
            log.warn("❌ [TimeCapsuleService] JWT 검증 실패! 401 Unauthorized 반환");
            return new CreateTimeCapsuleResponseDto("AUTH_REQUIRED", "Authentication token is required.", null);
        }

        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            log.warn("❌ [TimeCapsuleService] 사용자 없음 - ID: {}", userId);
            return new CreateTimeCapsuleResponseDto("USER_NOT_FOUND", "User not found.", null);
        }

        try {
            // 문자열을 Instant로 변환 (문자열 형식이 ISO-8601 형식이어야 합니다)
            Instant openedAtInstant = Instant.parse(request.getOpenedAt());

            // openedAtInstant가 현재 시각보다 이전인지 확인
            if (openedAtInstant.isBefore(Instant.now())) {
                log.warn("❌ [TimeCapsuleService] 타임캡슐을 과거에 심음 - 심은 시각: {} 현재 시각: {}",
                        openedAtInstant, Instant.now());
                return new CreateTimeCapsuleResponseDto("INVALID_OPENED_AT", "OpenedAt must be a future date.", null);
            }
        } catch (DateTimeParseException e) {
            log.error("❌ [TimeCapsuleService] openedAt 변환 실패: {} 형식이 올바르지 않습니다. 입력값: {}",
                    request.getOpenedAt(), e.getMessage());
        }

        User user = userOpt.get();

        // ✅ 타임캡슐 저장
        TimeCapsule timeCapsule = TimeCapsule.builder()
                .user(user)
                .title(request.getTitle())
                .content(request.getContent())
                .openedAt(Instant.parse(request.getOpenedAt()))
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();
        timeCapsule = timeCapsuleRepository.save(timeCapsule);
        log.info("✅ [TimeCapsuleService] 타임캡슐 저장 완료 - ID: {}", timeCapsule.getCapsuleId());

        // ✅ S3 업로드 및 멀티미디어 저장
        List<String> mediaUrls = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                String uploadedUrl = awsS3Service.uploadFile(file);
                mediaUrls.add(uploadedUrl);
                timeCapsuleMediaRepository.save(new TimeCapsuleMedia(null, timeCapsule, uploadedUrl));
            }
        }
        log.info("✅ [TimeCapsuleService] 멀티미디어 저장 완료 - {}개", mediaUrls.size());

        // ✅ 공유 사용자 저장 (자신 포함)
        List<Integer> accessUserIds = new ArrayList<>();
        if (request.getAccessUserIds() != null) {
            accessUserIds.addAll(request.getAccessUserIds());
        }
        accessUserIds.add(userId); // 본인도 포함

        for (Integer accessUserId : accessUserIds) {
            User accessUser = userRepository.findById(accessUserId).orElse(null);
            if (accessUser != null) {
                timeCapsuleAccessUserRepository.save(new TimeCapsuleAccessUser(accessUser, timeCapsule,0));
            }
        }
        log.info("✅ [TimeCapsuleService] 공유 사용자 저장 완료 - {}명", accessUserIds.size());

        // ✅ 응답 반환
        return new CreateTimeCapsuleResponseDto(
                "SUCCESS",
                "Time capsule created successfully.",
                CreateTimeCapsuleResponseDto.Data.builder()
                        .capsuleId(timeCapsule.getCapsuleId())
                        .userId(userId)
                        .title(timeCapsule.getTitle())
                        .content(timeCapsule.getContent())
                        .openedAt(timeCapsule.getOpenedAt())
                        .latitude(timeCapsule.getLatitude())
                        .longitude(timeCapsule.getLongitude())
                        .multimediaUrls(mediaUrls)
                        .accessUserIds(accessUserIds)
                        .build()
        );
    }

    @Transactional(readOnly = true)
    public GetTimeCapsuleMapResponseDto getTimeCapsulesOnMap(String token, GetTimeCapsuleMapRequestDto request) {
        Integer userId = jwtProvider.validateToken(token);
        if (userId == null) {
            log.warn("❌ [Service] JWT 검증 실패! Unauthorized");
            return new GetTimeCapsuleMapResponseDto("UA", "Unauthorized. Please provide a valid token.", List.of());
        }

        log.info("🔹 [Service] 사용자 ID: {}의 타임캡슐 조회 요청 - 좌표 범위: left={}, right={}, up={}, down={}",
                userId, request.getLeft(), request.getRight(), request.getUp(), request.getDown());

        List<TimeCapsule> capsules = timeCapsuleRepository.findAccessibleCapsulesInBounds(
                userId, request.getLeft(), request.getRight(), request.getUp(), request.getDown());

        if (capsules.isEmpty()) {
            log.warn("⚠ [Service] 검색된 타임캡슐 없음");
            return new GetTimeCapsuleMapResponseDto("NO_DATA", "No available time capsules found.", List.of());
        }

        List<GetTimeCapsuleMapResponseDto.TimeCapsuleData> capsuleDataList = capsules.stream()
                .map(tc -> new GetTimeCapsuleMapResponseDto.TimeCapsuleData(
                        tc.getCapsuleId(), tc.getTitle(), tc.getLatitude(), tc.getLongitude(),
                        tc.getOpenedAt().toString().substring(0, 10) // 날짜 형식 변환
                ))
                .collect(Collectors.toList());

        log.info("✅ [Service] 타임캡슐 {}개 검색 완료", capsuleDataList.size());
        return GetTimeCapsuleMapResponseDto.success(capsuleDataList);
    }

    @Override
    @Transactional(readOnly = true)
    public GetTimeCapsuleMapListResponseDto getAccessibleTimeCapsules(String token) {
        Integer userId = jwtProvider.validateToken(token);
        if (userId == null) return GetTimeCapsuleMapListResponseDto.unauthorized();

        log.info("🔍 [타임캡슐 지도 조회] userId={}", userId);

        // ✅ 접근 가능한 타임캡슐 조회
        List<TimeCapsule> timeCapsules = timeCapsuleRepository.findAccessibleTimeCapsules(userId);
        log.info("✅ [타임캡슐 조회 완료] count={}", timeCapsules.size());

        // ✅ 응답 데이터 변환
        List<GetTimeCapsuleMapListResponseDto.Data> responseData = timeCapsules.stream()
                .map(tc -> new GetTimeCapsuleMapListResponseDto.Data(
                        tc.getCapsuleId(),
                        tc.getTitle(),
                        tc.getLatitude(),
                        tc.getLongitude(),
                        tc.getOpenedAt().toString()
                ))
                .collect(Collectors.toList());

        return GetTimeCapsuleMapListResponseDto.success(responseData);
    }

    @Override
    @Transactional(readOnly = true)
    public TimeCapsuleDetailResponseDto getTimeCapsuleDetail(String token, Integer capsuleId) {
        Integer userId = jwtProvider.validateToken(token);
        if (userId == null) return TimeCapsuleDetailResponseDto.unauthorized();
        if (capsuleId == null) return TimeCapsuleDetailResponseDto.missingCapsuleId();

        log.info("🔍 [타임캡슐 상세 조회] userId={}, capsuleId={}", userId, capsuleId);

        // ✅ 타임캡슐 기본 정보 조회
        Optional<TimeCapsule> optionalCapsule = timeCapsuleRepository.findByCapsuleId(capsuleId);
        if (optionalCapsule.isEmpty()) return TimeCapsuleDetailResponseDto.notFound();
        TimeCapsule capsule = optionalCapsule.get();

        // ✅ 접근 권한 확인
        boolean hasAccess = timeCapsuleAccessUserRepository.existsByUserUserIdAndTimeCapsuleCapsuleId(userId, capsuleId);
        if (!hasAccess) return TimeCapsuleDetailResponseDto.accessDenied();

        // ✅ 공유된 사용자 목록 조회
        List<TimeCapsuleDetailResponseDto.SharedUser> sharedUsers = timeCapsuleAccessUserRepository.findByTimeCapsuleCapsuleId(capsuleId)
                .stream()
                .map(accessUser -> new TimeCapsuleDetailResponseDto.SharedUser(
                        accessUser.getUser().getUserId(),
                        accessUser.getUser().getNickname(),
                        accessUser.getUser().getMediaUrl()
                ))
                .collect(Collectors.toList());

        // ✅ 미디어 URL 목록 조회
        List<String> mediaUrls = timeCapsuleMediaRepository.findByTimeCapsule_CapsuleId(capsuleId)
                .stream()
                .map(TimeCapsuleMedia::getMediaUrl)
                .collect(Collectors.toList());

        return TimeCapsuleDetailResponseDto.success(
                capsule.getCapsuleId(),
                capsule.getTitle(),
                capsule.getContent(),
                capsule.getCreatedAt().toString(),
                capsule.getOpenedAt() != null ? capsule.getOpenedAt().toString() : null,
                capsule.getLatitude(),
                capsule.getLongitude(),
                new TimeCapsuleDetailResponseDto.Author(
                        capsule.getUser().getUserId(),
                        capsule.getUser().getNickname(),
                        capsule.getUser().getMediaUrl()
                ),
                sharedUsers,
                mediaUrls
        );
    }

    @Override
    @Transactional
    public TimeCapsuleDeleteResponseDto deleteTimeCapsule(String token, TimeCapsuleDeleteRequestDto request) {
        Integer userId = jwtProvider.validateToken(token);
        if (userId == null) return TimeCapsuleDeleteResponseDto.unauthorized();
        Integer capsuleId=request.getCapsuleId();
        log.info("🔍 [타임캡슐 삭제 요청] userId={}, capsuleId={}", userId, capsuleId);

        // ✅ 필수 필드 누락 검사
        if (request.getCapsuleId() == null) return TimeCapsuleDeleteResponseDto.missingCapsuleId();

        // ✅ 타임캡슐 존재 여부 확인
        if (!timeCapsuleRepository.existsById(request.getCapsuleId())) {
            return TimeCapsuleDeleteResponseDto.timeCapsuleNotFound();
        }

        // ✅ 본인 소유인지 확인
        if (!timeCapsuleRepository.existsByCapsuleIdAndUserUserId(request.getCapsuleId(), userId)) {
            return TimeCapsuleDeleteResponseDto.accessDenied();
        }
        // ✅ 3. `time_capsule_media`에서 S3 URL 가져오기
        List<String> mediaUrls = timeCapsuleMediaRepository.findMediaUrlsByCapsuleId(capsuleId);
        log.info("📌 [S3 삭제 대상 파일] capsuleId={}, 파일 개수={}", request.getCapsuleId(), mediaUrls.size());

        // ✅ 4. S3에서 파일 삭제
        for (String url : mediaUrls) {
            awsS3Service.deleteFileFromS3(url);
        }

        // ✅ 타임캡슐 삭제
        timeCapsuleRepository.deleteByCapsuleId(request.getCapsuleId());
        log.info("✅ [타임캡슐 삭제 완료] capsuleId={}", request.getCapsuleId());

        return TimeCapsuleDeleteResponseDto.success(request.getCapsuleId());
    }

    @Override
    @Transactional
    public TimeCapsuleCollectResponseDto collectTimeCapsules(String token) {
        Integer userId = jwtProvider.validateToken(token);
        if (userId == null) return TimeCapsuleCollectResponseDto.unauthorized();

        log.info("🔍 [타임캡슐 수거 조회 요청] userId={}", userId);

        // ✅ 현재 시간을 기준으로 수거 가능한 타임캡슐 조회
        List<TimeCapsule> collectableCapsules =
                timeCapsuleAccessUserRepository.findCollectableCapsules(userId, Instant.now());

        if (collectableCapsules.isEmpty()) {
            log.info("❌ [수거할 타임캡슐 없음] userId={}", userId);
            return TimeCapsuleCollectResponseDto.noCollectableCapsules();
        }

        // ✅ 해당 타임캡슐의 is_read 상태 업데이트 (수거 처리)
        List<Integer> capsuleIds = collectableCapsules.stream()
                .map(TimeCapsule::getCapsuleId)
                .collect(Collectors.toList());
        timeCapsuleAccessUserRepository.markCapsulesAsCollected(userId, capsuleIds);

        log.info("✅ [타임캡슐 수거 완료] userId={}, capsuleIds={}", userId, capsuleIds);

        return TimeCapsuleCollectResponseDto.success(collectableCapsules);
    }
}
