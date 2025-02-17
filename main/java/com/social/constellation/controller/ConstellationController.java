package com.social.constellation.controller;

import com.social.constellation.dto.request.CreateConstellationRequestDto;
import com.social.constellation.dto.request.DeleteConstellationRequestDto;
import com.social.constellation.dto.request.GetConstellationNameRequestDto;
import com.social.constellation.dto.response.CreateConstellationResponseDto;
import com.social.constellation.dto.response.DeleteConstellationResponseDto;
import com.social.constellation.dto.response.GetConstellationNameResponseDto;
import com.social.constellation.service.ConstellationService;
import com.social.login.provider.JWTProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/constellations")
@RequiredArgsConstructor
public class ConstellationController {

    private final ConstellationService constellationService;
    private final JWTProvider jwtProvider;

    @PostMapping
    public ResponseEntity<CreateConstellationResponseDto> createConstellation(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody @Valid CreateConstellationRequestDto requestDto) {

        String token = authorizationHeader.replace("Bearer ", "");
        Integer userId = jwtProvider.validateToken(token); // ✅ JWT 검증

        return ResponseEntity.ok(constellationService.createConstellation(userId, requestDto));
    }

    @PostMapping("/name")
    public ResponseEntity<GetConstellationNameResponseDto> getConstellationName(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody @Valid GetConstellationNameRequestDto requestDto) {

        String token = authorizationHeader.replace("Bearer ", "");
        Integer userId = jwtProvider.validateToken(token); // ✅ JWT 검증

        return ResponseEntity.ok(constellationService.getConstellationName(requestDto));
    }

    @PostMapping("/delete")
    public ResponseEntity<DeleteConstellationResponseDto> deleteConstellation(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody @Valid DeleteConstellationRequestDto requestDto) {

        String token = authorizationHeader.replace("Bearer ", "");
        Integer userId = jwtProvider.validateToken(token); // ✅ JWT 검증

        return ResponseEntity.ok(constellationService.deleteConstellation(userId, requestDto));
    }
}
