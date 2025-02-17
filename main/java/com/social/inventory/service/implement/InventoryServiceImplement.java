package com.social.inventory.service.implement;

import com.social.inventory.dto.response.ClothesInventoryResponseDto;
import com.social.inventory.dto.response.GetClothesInventoryResponseDto;
import com.social.inventory.dto.response.TimeCapsuleInventoryResponseDto;
import com.social.inventory.entity.Costume;
import com.social.inventory.repository.CostumeRepository;
import com.social.inventory.repository.ItemRepository;
import com.social.inventory.service.InventoryService;
import com.social.login.entity.User;
import com.social.login.provider.JWTProvider;
import com.social.login.repository.UserRepository;
import com.social.timecapsules.entity.TimeCapsule;
import com.social.timecapsules.repository.TimeCapsuleAccessUserRepository;
import com.social.timecapsules.repository.TimeCapsuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImplement implements InventoryService {

    private final CostumeRepository costumeRepository;
    private final UserRepository userRepository;
    private final JWTProvider jwtProvider;
    private final ItemRepository itemRepository;
    private final TimeCapsuleAccessUserRepository timeCapsuleAccessUserRepository;
    private final TimeCapsuleRepository timeCapsuleRepository;

    @Override
    @Transactional(readOnly = true)
    public GetClothesInventoryResponseDto getClothesInventory(String token) {
        log.info("📌 [가방 - 옷 조회 서비스 시작]");

        Integer userId = jwtProvider.validateToken(token);
        if (userId == null) {
            log.warn("❌ [옷 조회 실패] 유효하지 않은 토큰");
            return GetClothesInventoryResponseDto.unauthorized();
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.error("❌ [옷 조회 실패] 서버 오류 - 사용자 조회 실패");
            return GetClothesInventoryResponseDto.serverError();
        }

        List<Costume> costumes = costumeRepository.findByUserUserIdOrderByHasItemDescItemItemIdAsc(userId);
        log.info("✅ [옷 조회 완료] 조회된 아이템 수={}", costumes.size());

        return GetClothesInventoryResponseDto.success(
                costumes.stream()
                        .map(c -> new GetClothesInventoryResponseDto.Data(c.getItem().getItemId(), c.getHasItem()))
                        .collect(Collectors.toList())
        );
    }

    @Override
    @Transactional(readOnly = true)
    public ClothesInventoryResponseDto getClothesDetail(String token, Integer itemId) {
        log.info("📌 [옷 상세 조회] 토큰 검증 시작");

        Integer userId = jwtProvider.validateToken(token);
        if (userId == null) {
            log.warn("❌ [조회 실패] JWT 토큰 검증 실패");
            return ClothesInventoryResponseDto.unauthorized();
        }
        log.info("✅ [토큰 검증 성공] 사용자 ID: {}", userId);

        if (itemId == null) {
            log.warn("❌ [조회 실패] 필수 필드 누락 - item_id 없음");
            return ClothesInventoryResponseDto.missingItemId();
        }

        return itemRepository.findByItemId(itemId)
                .map(item -> {
                    log.info("✅ [옷 상세 조회 성공] 아이템 ID: {}, 이름: {}", itemId, item.getItemName());
                    return ClothesInventoryResponseDto.success(item.getItemName(), item.getDescription());
                })
                .orElseGet(() -> {
                    log.warn("❌ [조회 실패] 존재하지 않는 아이템 ID: {}", itemId);
                    return ClothesInventoryResponseDto.itemNotFound();
                });
    }

    @Override
    @Transactional(readOnly = true)
    public TimeCapsuleInventoryResponseDto getReadTimeCapsules(String token) {
        log.info("📌 [가방 - 읽은 타임캡슐 조회] 토큰 검증 시작");

        Integer userId = jwtProvider.validateToken(token);
        if (userId == null) {
            log.warn("❌ [조회 실패] JWT 토큰 검증 실패");
            return TimeCapsuleInventoryResponseDto.unauthorized();
        }
        log.info("✅ [토큰 검증 성공] 사용자 ID: {}", userId);

        log.info("📌 [타임캡슐 조회] 사용자 ID: {}의 읽은 타임캡슐 목록 조회 중...", userId);
        List<Integer> capsuleIds = timeCapsuleAccessUserRepository.findReadCapsuleIdsByUserId(userId);

        if (capsuleIds.isEmpty()) {
            log.info("✅ [조회 완료] 사용자가 읽은 타임캡슐 없음");
            return TimeCapsuleInventoryResponseDto.noCapsulesFound();
        }

        log.info("📌 [타임캡슐 조회] 총 {}개의 캡슐 ID 조회됨: {}", capsuleIds.size(), capsuleIds);

        List<TimeCapsule> capsules = timeCapsuleRepository.findByCapsuleIdInOrderByOpenedAtDesc(capsuleIds);
        log.info("✅ [조회 완료] {}개의 타임캡슐 정보 반환", capsules.size());

        return TimeCapsuleInventoryResponseDto.success(
                capsules.stream()
                        .map(capsule -> new TimeCapsuleInventoryResponseDto.Data(
                                capsule.getCapsuleId(),
                                capsule.getTitle()
                        ))
                        .collect(Collectors.toList())
        );
    }
}
