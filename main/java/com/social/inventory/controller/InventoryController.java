package com.social.inventory.controller;

import com.social.inventory.dto.response.ClothesInventoryResponseDto;
import com.social.inventory.dto.response.GetClothesInventoryResponseDto;
import com.social.inventory.dto.response.TimeCapsuleInventoryResponseDto;
import com.social.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@Slf4j
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping("/clothes")
    public ResponseEntity<GetClothesInventoryResponseDto> getClothesInventory(
            @RequestHeader("Authorization") String authorization) {

        log.info("📌 [가방 - 옷 조회 요청] Authorization 헤더 수신");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("❌ [옷 조회 실패] Authorization 헤더가 없음 또는 형식 오류");
            return ResponseEntity.status(401).body(GetClothesInventoryResponseDto.unauthorized());
        }

        String token = authorization.replace("Bearer ", "");
        log.info("✅ [토큰 추출 완료] token={}", token);

        log.info("📌 [옷 조회 서비스 호출]");
        GetClothesInventoryResponseDto response = inventoryService.getClothesInventory(token);

        if ("SUCCESS".equals(response.getCode())) {
            log.info("✅ [옷 조회 성공] 조회된 아이템 수={}", response.getData().size());
            return ResponseEntity.ok(response);
        } else {
            log.warn("❌ [옷 조회 실패] 이유: {}", response.getMessage());
            return ResponseEntity.status(400).body(response);
        }
    }

    @GetMapping("/detail")
    public ResponseEntity<ClothesInventoryResponseDto> getClothesDetail(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("item_id") Integer itemId) {

        log.info("📌 [가방 - 옷 상세 조회 API 호출] Authorization 헤더 수신");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("❌ [조회 실패] Authorization 헤더가 없음 또는 형식 오류");
            return ResponseEntity.status(401).body(ClothesInventoryResponseDto.unauthorized());
        }

        String token = authorization.replace("Bearer ", "");
        log.info("✅ [토큰 추출 완료] token={}", token);

        ClothesInventoryResponseDto response = inventoryService.getClothesDetail(token, itemId);

        if ("SUCCESS".equals(response.getCode())) {
            log.info("✅ [조회 성공] item_id: {}", itemId);
            return ResponseEntity.ok(response);
        } else {
            log.warn("❌ [조회 실패] item_id: {}, 이유: {}", itemId, response.getMessage());
            return ResponseEntity.status(404).body(response);
        }
    }

    @GetMapping("/time-capsules")
    public ResponseEntity<TimeCapsuleInventoryResponseDto> getReadTimeCapsules(
            @RequestHeader("Authorization") String authorization) {

        log.info("📌 [가방 - 타임캡슐 조회 API 호출] Authorization 헤더 수신");

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("❌ [조회 실패] Authorization 헤더가 없음 또는 형식 오류");
            return ResponseEntity.status(401).body(TimeCapsuleInventoryResponseDto.unauthorized());
        }

        String token = authorization.replace("Bearer ", "");
        log.info("✅ [토큰 추출 완료] token={}", token);

        TimeCapsuleInventoryResponseDto response = inventoryService.getReadTimeCapsules(token);

        if ("SUCCESS".equals(response.getCode())) {
            log.info("✅ [조회 성공] {}개의 타임캡슐 반환", response.getData().size());
            return ResponseEntity.ok(response);
        } else {
            log.warn("❌ [조회 실패] 이유: {}", response.getMessage());
            return ResponseEntity.status(401).body(response);
        }
    }
}
