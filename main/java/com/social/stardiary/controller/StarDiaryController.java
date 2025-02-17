package com.social.stardiary.controller;

import com.social.login.provider.JWTProvider;
import com.social.stardiary.dto.request.CreateStarDiaryRequestDto;
import com.social.stardiary.dto.request.DeleteStarDiaryRequestDto;
import com.social.stardiary.dto.request.GetStarDiaryRequestDto;
import com.social.stardiary.dto.response.CreateStarDiaryResponseDto;
import com.social.stardiary.dto.response.DeleteStarDiaryResponseDto;
import com.social.stardiary.dto.response.GetStarDiaryResponseDto;
import com.social.stardiary.service.StarDiaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/star-diary")
@RequiredArgsConstructor
public class StarDiaryController {

    private final StarDiaryService starDiaryService;
    private final JWTProvider jwtProvider;

    @PostMapping
    public ResponseEntity<CreateStarDiaryResponseDto> createStarDiary(
            @RequestHeader("Authorization") String authorizationHeader,
            @ModelAttribute @Valid CreateStarDiaryRequestDto requestDto) {
        return ResponseEntity.ok(starDiaryService.createStarDiary(requestDto));
    }

    @PostMapping("/detail")
    public ResponseEntity<GetStarDiaryResponseDto> getStarDiary(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody @Valid GetStarDiaryRequestDto requestDto) { // ✅ starId를 JSON Body에서 받음
        // JWT 토큰에서 userId 추출
        String token = authorizationHeader.replace("Bearer ", "");
        Integer userId = jwtProvider.getUserIdFromToken(token);
        starDiaryService.checkOwnership(userId,requestDto.getStarId());

        return ResponseEntity.ok(starDiaryService.getStarDiaryByStarId(requestDto.getStarId()));
    }

    @PostMapping("/delete")
    public ResponseEntity<DeleteStarDiaryResponseDto> deleteStarDiary(
            @RequestHeader("Authorization") String authorizationHeader,
            @RequestBody @Valid DeleteStarDiaryRequestDto requestDto) {

        String token = authorizationHeader.replace("Bearer ", "");
        Integer userId = jwtProvider.validateToken(token); // ✅ JWT 검증

        return ResponseEntity.ok(starDiaryService.deleteStarDiary(userId, requestDto.getStarId()));
    }
}
