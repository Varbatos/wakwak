package com.social.login.provider;

import com.social.login.entity.User;
import com.social.login.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Slf4j
@Service
@ConfigurationProperties(prefix = "jwt") // application.properties에서 자동으로 secret-key 매핑
@RequiredArgsConstructor
public class JWTProvider {

    private final UserRepository userRepository;
    private Key key;  // final 제거

    // 🔹 Setter 추가 (Spring이 application.properties에서 값을 자동 주입)
    public void setSecretKey(String secretKey) {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalArgumentException("❌ [JWTProvider] secret-key가 설정되지 않았습니다!");
        }
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        log.info("✅ [JWTProvider] JWT 키 설정 완료");
    }

    /**
     * JWT 토큰 생성
     */
    public String createToken(String username) {
        // DB에서 username으로 userId 찾기
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("❌ [JWTProvider] 해당 유저를 찾을 수 없음: " + username);
        }

        Integer userId = user.getUserId(); // userId 가져오기
        //반드시 수정 필요
        Date expiredDate = Date.from(Instant.now().plus(1000, ChronoUnit.HOURS));

        return Jwts.builder()
                .signWith(key, SignatureAlgorithm.HS256)
                .setSubject(userId.toString()) // userId를 Subject로 저장
                .setIssuedAt(new Date())
                .setExpiration(expiredDate)
                .compact();
    }

    /**
     * JWT 토큰 검증 및 사용자 ID 반환
     */
    public Integer validateToken(String jwt) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(jwt)
                    .getBody();

            String userIdString = claims.getSubject();
            Integer userId = Integer.parseInt(userIdString);

            log.info("✅ [JWTProvider] JWT 검증 성공 - 사용자 ID: {}", userId);
            return userId;
        } catch (Exception exception) {
            log.error("❌ [JWTProvider] JWT 검증 실패: {}", exception.getMessage(), exception);
            return null;
        }
    }

    /**
     * JWT에서 사용자 ID 추출
     */
    public Integer getUserIdFromToken(String token) {
        return validateToken(token);
    }
}
