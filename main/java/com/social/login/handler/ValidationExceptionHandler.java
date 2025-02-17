package com.social.login.handler;

import com.social.login.dto.response.ResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ValidationExceptionHandler {

    /**
     * ❌ 입력값 검증 실패 (MethodArgumentNotValidException, HttpMessageNotReadableException)
     * - MethodArgumentNotValidException: @Valid 검증 실패
     * - HttpMessageNotReadableException: JSON 요청 형식 오류
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ResponseDto> validationExceptionHandler(Exception ex, HttpServletRequest request) {
        log.warn("❌ [ValidationExceptionHandler] 요청 검증 실패 - URL: {}, 오류: {}",
                request.getRequestURI(), ex.getMessage());

        log.debug("🔹 [ValidationExceptionHandler] 전체 예외 정보", ex);
        return ResponseDto.validationFail();
    }
}
