package com.social.timecapsules.controller;

import com.social.timecapsules.dto.request.CreateTimeCapsuleRequestDto;
import com.social.timecapsules.dto.request.GetTimeCapsuleMapRequestDto;
import com.social.timecapsules.dto.request.TimeCapsuleDeleteRequestDto;
import com.social.timecapsules.dto.response.*;
import com.social.timecapsules.service.TimeCapsuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/time-capsules")
@RequiredArgsConstructor
public class TimeCapsuleController {

    private final TimeCapsuleService timeCapsuleService;

    @PostMapping
    public ResponseEntity<CreateTimeCapsuleResponseDto> createTimeCapsule(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @ModelAttribute CreateTimeCapsuleRequestDto request,
            @RequestPart(name = "files", required = false) List<MultipartFile> files) {

        log.info("🔹 [Controller] 타임캡슐 생성 요청: title={}, openedAt={}, latitude={}, longitude={}",
                request.getTitle(), request.getOpenedAt(), request.getLatitude(), request.getLongitude());

        String token = authorizationHeader.replace("Bearer ", "");
        CreateTimeCapsuleResponseDto response = timeCapsuleService.createTimeCapsule(token, request, files);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/map")
    public ResponseEntity<GetTimeCapsuleMapResponseDto> getTimeCapsulesOnMap(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid GetTimeCapsuleMapRequestDto request
    ) {
        log.info("🔹 [Controller] /time-capsules/map GET 요청 - 좌표 범위: left={}, right={}, up={}, down={}",
                request.getLeft(), request.getRight(), request.getUp(), request.getDown());

        String token = authorizationHeader.replace("Bearer ", "");
        GetTimeCapsuleMapResponseDto response = timeCapsuleService.getTimeCapsulesOnMap(token, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/map/list")
    public ResponseEntity<GetTimeCapsuleMapListResponseDto> getTimeCapsules(
            @RequestHeader("Authorization") String authorization) {

        log.info("📌 [타임캡슐 지도 조회 API 호출]");

        String token = authorization.replace("Bearer ", "");
        GetTimeCapsuleMapListResponseDto response = timeCapsuleService.getAccessibleTimeCapsules(token);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<TimeCapsuleDetailResponseDto> getTimeCapsuleDetail(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("capsule_id") Integer capsuleId) {

        log.info("📌 [타임캡슐 상세 조회 API 호출] capsuleId={}", capsuleId);

        String token = authorization.replace("Bearer ", "");
        TimeCapsuleDetailResponseDto response = timeCapsuleService.getTimeCapsuleDetail(token, capsuleId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/delete")
    public ResponseEntity<TimeCapsuleDeleteResponseDto> deleteTimeCapsule(
            @RequestHeader("Authorization") String authorization,
            @RequestBody TimeCapsuleDeleteRequestDto request) {

        log.info("📌 [타임캡슐 삭제 API 호출]");

        String token = authorization.replace("Bearer ", "");
        TimeCapsuleDeleteResponseDto response = timeCapsuleService.deleteTimeCapsule(token, request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/map/collect")
    public ResponseEntity<TimeCapsuleCollectResponseDto> collectTimeCapsules(
            @RequestHeader("Authorization") String authorization) {

        log.info("📌 [타임캡슐 수거 API 호출]");

        String token = authorization.replace("Bearer ", "");
        TimeCapsuleCollectResponseDto response = timeCapsuleService.collectTimeCapsules(token);

        return ResponseEntity.ok(response);
    }
}
