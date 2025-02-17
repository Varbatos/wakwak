package com.social.login.controller;

import com.social.login.dto.request.*;
import com.social.login.dto.response.auth.*;
import com.social.login.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ✅ ID 중복 확인
    @PostMapping("/check-id")
    public ResponseEntity<? super CheckIdResponseDto> idCheck(
            @RequestBody @Valid CheckIdRequestDto requestBody
    ) {
        log.info("🔹 [AuthController] /users/check-id 요청 수신 - ID: {}", requestBody.getId());

        ResponseEntity<? super CheckIdResponseDto> response = authService.idCheck(requestBody);
        log.info("✅ [AuthController] ID 중복 확인 완료 - 응답: {}", response.getStatusCode());

        return response;
    }


    // ✅ Nickname 중복 확인
    @PostMapping("/check-nickname")
    public ResponseEntity<? super CheckNicknameResponseDto> idCheck(
            @RequestBody @Valid CheckNicknameRequestDto requestBody
    ) {
        log.info("🔹 [AuthController] /users/check-id 요청 수신 - ID: {}", requestBody.getNickname());

        ResponseEntity<? super CheckNicknameResponseDto> response = authService.nicknameCheck(requestBody);
        log.info("✅ [AuthController] Nickname 중복 확인 완료 - 응답: {}", response.getStatusCode());

        return response;
    }

    // ✅ 이메일 인증 코드 발송
    @PostMapping("/send-verification-code")
    public ResponseEntity<? super SendVerificationCodeResponseDto> emailCertification(
            @RequestBody @Valid SendVerificationCodeRequestDto requestBody
    ) {
        log.info("🔹 [AuthController] /users/send-verification-code 요청 수신 - 이메일: {}", requestBody.getEmail());

        ResponseEntity<? super SendVerificationCodeResponseDto> response = authService.emailCertification(requestBody);
        log.info("✅ [AuthController] 인증 코드 발송 완료 - 응답: {}", response.getStatusCode());

        return response;
    }

    // ✅ 이메일 코드 검증
    @PostMapping("/verify-email-code")
    public ResponseEntity<? super VerifyEmailCodeResponseDto> checkCertification(
            @RequestBody @Valid VerifyEmailCodeRequestDto requestBody
    ) {
        log.info("🔹 [AuthController] /users/verify-email-code 요청 수신 - 이메일: {}, 인증코드: {}",
                requestBody.getEmail(), requestBody.getCertificationNumber());

        ResponseEntity<? super VerifyEmailCodeResponseDto> response = authService.checkCertification(requestBody);
        log.info("✅ [AuthController] 이메일 코드 검증 완료 - 응답: {}", response.getStatusCode());

        return response;
    }

    // ✅ 회원가입
    @PostMapping("/signup")
    public ResponseEntity<? super SignUpResponseDto> signUp(
            @RequestBody @Valid SignUpRequestDto requestBody
    ) {
        log.info("🔹 [AuthController] /users/signup 요청 수신 - 사용자명: {}", requestBody.getId());

        ResponseEntity<? super SignUpResponseDto> response = authService.signUp(requestBody);
        log.info("✅ [AuthController] 회원가입 완료 - 응답: {}", response.getStatusCode());

        return response;
    }

    // ✅ 로그인
    @PostMapping("/login")
    public ResponseEntity<? super LoginResponseDto> signIn(
            @RequestBody @Valid LoginRequestDto requestBody
    ) {
        log.info("🔹 [AuthController] /users/login 요청 수신 - 사용자명: {}", requestBody.getId());

        ResponseEntity<? super LoginResponseDto> response = authService.signIn(requestBody);
        log.info("✅ [AuthController] 로그인 완료 - 응답: {}", response.getStatusCode());

        return response;
    }

    @PatchMapping("/nickname")
    public ResponseEntity<NicknameUpdateResponseDto> updateNickname(
            @RequestHeader("Authorization") String authorization,
            @RequestBody NicknameUpdateRequestDto request) {

        log.info("📌 [닉네임 변경 API 호출] 새로운 닉네임: {}", request.getNickname());

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("❌ [닉네임 변경 실패] Authorization 헤더 없음 또는 형식 오류");
            return ResponseEntity.status(401).body(NicknameUpdateResponseDto.unauthorized());
        }

        String token = authorization.replace("Bearer ", "");
        log.info("✅ [토큰 추출 완료] token={}", token);

        NicknameUpdateResponseDto response = authService.updateNickname(token, request.getNickname());
        return ResponseEntity.status(response.getHttpStatus()).body(response);
    }

    @PatchMapping("/profile-image")
    public ResponseEntity<ProfileImageUpdateResponseDto> updateProfileImage(
            @RequestHeader("Authorization") String authorization,
            @RequestParam("profile_image") MultipartFile file) {

        log.info("📌 [프로필 이미지 변경 API 호출] 파일 크기: {} bytes", file.getSize());

        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.warn("❌ [프로필 이미지 변경 실패] Authorization 헤더 없음 또는 형식 오류");
            return ResponseEntity.status(401).body(ProfileImageUpdateResponseDto.unauthorized());
        }

        String token = authorization.replace("Bearer ", "");
        log.info("✅ [토큰 추출 완료] token={}", token);

        ProfileImageUpdateResponseDto response = authService.updateProfileImage(token, file);
        return ResponseEntity.status(response.getHttpStatus()).body(response);
    }

    @PostMapping("/device")
    public ResponseEntity<RegisterDeviceResponseDto> registerDevice(
            @RequestHeader("Authorization") String authorization,
            @RequestBody RegisterDeviceRequestDto requestDto) {

        log.info("📌 [디바이스 등록 요청] Device ID: {}, Device Name: {}", requestDto.getDeviceId(), requestDto.getDeviceName());

        String token = authorization.replace("Bearer ", "");
        log.info("✅ [토큰 추출 완료] token={}", token);
        RegisterDeviceResponseDto response = authService.registerDevice(token, requestDto);

        log.info("✅ [디바이스 등록 완료] Device ID: {}, User ID: {}", response.getData().getDeviceId(), response.getData().getUserId());

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<GetUserResponseDto> getUserProfile(@RequestHeader("Authorization") String token) {
        log.info("📌 [회원정보 조회 요청]");

        // JWT Token "Bearer " 제거
        String jwtToken = token.replace("Bearer ", "");

        GetUserResponseDto response = authService.getUserProfile(jwtToken);

        return ResponseEntity.ok(response);
    }

}
