package com.social.login.service;

import com.social.login.dto.request.*;
import com.social.login.dto.response.auth.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

public interface AuthService {

    ResponseEntity<? super CheckIdResponseDto> idCheck(CheckIdRequestDto dto);
    ResponseEntity<? super CheckNicknameResponseDto> nicknameCheck(CheckNicknameRequestDto dto);
    ResponseEntity<? super SendVerificationCodeResponseDto> emailCertification(SendVerificationCodeRequestDto dto);
    ResponseEntity<? super VerifyEmailCodeResponseDto> checkCertification(VerifyEmailCodeRequestDto dto);
    ResponseEntity<? super SignUpResponseDto> signUp(SignUpRequestDto dto);
    ResponseEntity<? super LoginResponseDto> signIn(LoginRequestDto dto);
    void updateUserConstellation(Integer userId, Integer minStarskyId);
    NicknameUpdateResponseDto updateNickname(String token, String nickname);

    ProfileImageUpdateResponseDto updateProfileImage(String token, MultipartFile file);

    RegisterDeviceResponseDto registerDevice(String token, RegisterDeviceRequestDto requestDto);

    GetUserResponseDto getUserProfile(String token);
}
