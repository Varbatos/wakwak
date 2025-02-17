package com.social.starsky.controller;

import com.social.login.provider.JWTProvider;
import com.social.starsky.dto.request.GetStarSkyConstellationsRequestDto;
import com.social.starsky.dto.request.GetStarSkyEquipRequestDto;
import com.social.starsky.dto.request.GetStarsBySkyRequestDto;
import com.social.starsky.dto.response.*;
import com.social.starsky.service.StarSkyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/star-sky")
@RequiredArgsConstructor
@Slf4j
public class StarSkyController {

    private final StarSkyService starSkyService;
    private final JWTProvider jwtProvider; // JWT 토큰 유틸리티

    @GetMapping("/list")
    public ResponseEntity<GetStarSkyListResponseDto> getUserStarSkyList(
            @RequestHeader("Authorization") String authorizationHeader// JWT 토큰을 헤더에서 받음
    ) {

        // JWT 토큰에서 userId 추출
        String token = authorizationHeader.replace("Bearer ", "");
        Integer userId = jwtProvider.getUserIdFromToken(token);

        // Service 호출
        GetStarSkyListResponseDto response = starSkyService.getUserStarSkyList(userId);
        return ResponseEntity.ok(response);
    }
    @GetMapping()
    public ResponseEntity<GetStarSkyResponseDto> getUserStarSky(
            @RequestHeader("Authorization") String authorizationHeader// JWT 토큰을 헤더에서 받음
    ) {

        // JWT 토큰에서 userId 추출
        String token = authorizationHeader.replace("Bearer ", "");
        Integer userId = jwtProvider.getUserIdFromToken(token);

        // Service 호출
        GetStarSkyResponseDto response = starSkyService.getUserStarSky(userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/star")
    public ResponseEntity<GetStarsBySkyResponseDto> getStarsBySky(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody @Valid GetStarsBySkyRequestDto requestDto) {

        String token = authorizationHeader.replace("Bearer ", "");
        Integer userId = jwtProvider.validateToken(token); // ✅ JWT 검증

        return ResponseEntity.ok(starSkyService.getStarsBySky(userId, requestDto));
    }

    @PostMapping("/constellations")
    public ResponseEntity<GetStarSkyConstellationsResponseDto> getConstellationsBySky(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody @Valid GetStarSkyConstellationsRequestDto requestDto) {

        String token = authorizationHeader.replace("Bearer ", "");
        Integer userId = jwtProvider.validateToken(token);

        return ResponseEntity.ok(starSkyService.getConstellationsBySky(userId, requestDto));
    }

    @PostMapping("/equip")
    public ResponseEntity<GetStarSkyEquipResponseDto> equipStarSky(
            @RequestHeader("Authorization") String authorizationHeader,
            @Valid @RequestBody GetStarSkyEquipRequestDto requestDto
    ) {
        log.info("🔹 [Controller] 별 하늘 착용 요청 - skyId: {}", requestDto.getSkyId());

        String token = authorizationHeader.replace("Bearer ", "");
        return starSkyService.equipStarSky(token, requestDto);
    }
}
