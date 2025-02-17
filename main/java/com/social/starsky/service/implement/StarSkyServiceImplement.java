package com.social.starsky.service.implement;

import com.social.constellation.entity.Constellation;
import com.social.constellation.entity.ConstellationName;
import com.social.constellation.repository.ConstellationNameRepository;
import com.social.constellation.repository.ConstellationRepository;
import com.social.login.provider.JWTProvider;
import com.social.login.repository.UserRepository;
import com.social.stardiary.entity.Star;
import com.social.stardiary.repository.StarRepository;
import com.social.starsky.dto.request.GetStarSkyConstellationsRequestDto;
import com.social.starsky.dto.request.GetStarSkyEquipRequestDto;
import com.social.starsky.dto.request.GetStarsBySkyRequestDto;
import com.social.starsky.dto.response.*;
import com.social.starsky.entity.StarSky;
import com.social.starsky.repository.StarSkyRepository;
import com.social.starsky.service.StarSkyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StarSkyServiceImplement implements StarSkyService {

    private final StarSkyRepository starSkyRepository;
    private final UserRepository userRepository;
    private final StarRepository starRepository;
    private final ConstellationRepository constellationRepository;
    private final ConstellationNameRepository constellationNameRepository;
    private final JWTProvider jwtProvider;

    @Override
    public Integer getMinStarskyIdByUserId(Integer userId) {


        log.info("🔹 [StarSkyService] userId={}의 가장 작은 starskyid 조회 요청", userId);
        Integer minStarskyId = starSkyRepository.findMinStarskyIdByUserId(userId);

        if (minStarskyId!=0) {
            log.info("✅ [StarSkyService] userId={}의 최소 starskyid={}", userId, minStarskyId);
        } else {
            log.warn("⚠️ [StarSkyService] userId={}의 starskyid 없음", userId);
        }

        return minStarskyId;

    }

    @Override
    public GetStarSkyListResponseDto getUserStarSkyList(Integer userId) {
        List<StarSky> starSkyList = starSkyRepository.findByUser_UserId(userId);

        List<GetStarSkyListResponseDto.StarSkyData> responseData = starSkyList.stream()
                .map(starSky -> GetStarSkyListResponseDto.StarSkyData.builder()
                        .starSkyId(starSky.getSkyId())
                        .build())
                .collect(Collectors.toList());

        return GetStarSkyListResponseDto.success(responseData);
    }

    @Override
    public GetStarSkyResponseDto getUserStarSky(Integer userId) {
        int starSkyId = userRepository.findConstellationByUserId(userId);
        return GetStarSkyResponseDto.success(starSkyId);
    }

    @Override
    @Transactional(readOnly = true)
    public GetStarsBySkyResponseDto getStarsBySky(Integer userId, GetStarsBySkyRequestDto requestDto) {
        log.info("📌 [별 하늘 조회 요청] 사용자 ID: {}, skyId={}", userId, requestDto.getSkyId());

        // ✅ 1. `sky_id`가 존재하는지 확인
        StarSky starSky = starSkyRepository.findById(requestDto.getSkyId())
                .orElse(null);

        if (starSky == null) {
            log.warn("❌ [별 하늘 조회 실패] 존재하지 않는 skyId={}", requestDto.getSkyId());
            return GetStarsBySkyResponseDto.skyNotFound();
        }

        // ✅ 2. 사용자 소유 확인
        if (!starSky.getUser().getUserId().equals(userId)) {
            log.warn("❌ [별 하늘 접근 오류] 사용자 ID: {}, skyId: {}", userId, requestDto.getSkyId());
            return GetStarsBySkyResponseDto.forbidden();
        }

        // ✅ 3. 해당 `sky_id`에 속하는 별 목록 조회
        List<Star> stars = starRepository.findByStarSky_SkyId(requestDto.getSkyId());

        List<GetStarsBySkyResponseDto.StarData> starDataList = stars.stream()
                .map(star -> GetStarsBySkyResponseDto.StarData.builder()
                        .starId(star.getStarId())
                        .latitude(star.getLatitude())
                        .longitude(star.getLongitude())
                        .build())
                .collect(Collectors.toList());

        log.info("✅ [별 목록 조회 완료] skyId={}, 별 개수={}", requestDto.getSkyId(), starDataList.size());
        return GetStarsBySkyResponseDto.success(starDataList);
    }

    @Override
    @Transactional(readOnly = true)
    public GetStarSkyConstellationsResponseDto getConstellationsBySky(Integer userId, GetStarSkyConstellationsRequestDto requestDto) {
        log.info("📌 [별자리 조회 요청] 사용자 ID: {}, skyId={}", userId, requestDto.getSkyId());

        // ✅ 1. `sky_id`가 존재하는지 확인
        StarSky starSky = starSkyRepository.findById(requestDto.getSkyId()).orElse(null);

        if (starSky == null) {
            log.warn("❌ [별자리 조회 실패] 존재하지 않는 skyId={}", requestDto.getSkyId());
            return GetStarSkyConstellationsResponseDto.skyNotFound();
        }

        // ✅ 2. 사용자 소유 확인
        if (!starSky.getUser().getUserId().equals(userId)) {
            log.warn("❌ [별 하늘 접근 오류] 사용자 ID: {}, skyId: {}", userId, requestDto.getSkyId());
            return GetStarSkyConstellationsResponseDto.forbidden();
        }

        // ✅ 3. 해당 `sky_id`에 속하는 별 목록 조회
        List<Star> stars = starRepository.findByStarSky_SkyId(requestDto.getSkyId());

        // ✅ 4. 별 ID 리스트 추출
        List<Integer> starIds = stars.stream().map(Star::getStarId).collect(Collectors.toList());

        // ✅ 5. 별 ID로 해당 별이 속한 별자리 조회
        List<Constellation> constellations = constellationRepository.findByStar_StarIdIn(starIds);

        // ✅ 6. 별자리 이름 조회
        Map<Integer, String> constellationNames = constellationNameRepository.findAllById(
                constellations.stream().map(c -> c.getConstellationName().getConstellationId()).collect(Collectors.toList())
        ).stream().collect(Collectors.toMap(ConstellationName::getConstellationId, ConstellationName::getConstellationName));

        // ✅ 7. 데이터 변환
        Map<Integer, List<Constellation>> groupedConstellations = constellations.stream()
                .collect(Collectors.groupingBy(c -> c.getConstellationName().getConstellationId()));

        List<GetStarSkyConstellationsResponseDto.ConstellationData> constellationDataList = groupedConstellations.entrySet().stream()
                .map(entry -> {
                    Integer constellationId = entry.getKey(); // 현재 별자리 ID 저장

                    return GetStarSkyConstellationsResponseDto.ConstellationData.builder()
                            .constellationId(constellationId)
                            .constellationName(constellationNames.get(constellationId))
                            .stars(entry.getValue().stream()
                                    .map(c -> {
                                        Star star = c.getStar();
                                        return GetStarSkyConstellationsResponseDto.StarData.builder()
                                                .starId(star.getStarId())
                                                .latitude(star.getLatitude())
                                                .longitude(star.getLongitude())
                                                .order(constellationRepository.findStarOrder(star.getStarId(), constellationId)) // ✅ order 값 가져오기
                                                .build();
                                    })
                                    .sorted(Comparator.comparing(GetStarSkyConstellationsResponseDto.StarData::getOrder)) // ✅ order 기준 정렬
                                    .collect(Collectors.toList()))
                            .build();
                }).collect(Collectors.toList());



        log.info("✅ [별자리 목록 조회 완료] skyId={}, 별자리 개수={}", requestDto.getSkyId(), constellationDataList.size());
        return GetStarSkyConstellationsResponseDto.success(constellationDataList);
    }

    @Transactional
    public ResponseEntity<GetStarSkyEquipResponseDto> equipStarSky(String token, GetStarSkyEquipRequestDto requestDto) {
        // ✅ 1. JWT 검증 및 사용자 ID 추출
        Integer userId = jwtProvider.validateToken(token);
        if (userId == null) {
            log.warn("❌ [Service] 인증 실패 - 유효하지 않은 토큰");
            return GetStarSkyEquipResponseDto.authRequired();
        }

        Integer skyId = requestDto.getSkyId();
        log.info("🔹 [Service] 사용자 ID: {}가 별 하늘 착용 요청 - skyId: {}", userId, skyId);

        // ✅ 2. 별 하늘 존재 여부 확인
        Optional<StarSky> starSkyOpt = starSkyRepository.findById(skyId);
        if (starSkyOpt.isEmpty()) {
            log.warn("❌ [Service] skyId={} 별 하늘이 존재하지 않음", skyId);
            return GetStarSkyEquipResponseDto.skyNotFound();
        }

        // ✅ 3. 해당 별 하늘이 사용자 소유인지 검증
        Optional<StarSky> userStarSkyOpt = starSkyRepository.findByUserIdAndStarSkyId(userId, skyId);
        if (userStarSkyOpt.isEmpty()) {
            log.warn("❌ [Service] 사용자 ID: {}가 skyId={} 별 하늘을 소유하지 않음", userId, skyId);
            return GetStarSkyEquipResponseDto.forbiddenSkyAccess();
        }

        // ✅ 4. users 테이블의 `constellation` 값을 업데이트
        userRepository.updateUserConstellation(userId, skyId);
        log.info("✅ [Service] 사용자 ID: {}의 별 하늘 변경 완료 - skyId: {}", userId, skyId);

        return GetStarSkyEquipResponseDto.success(userId, skyId);
    }
}
