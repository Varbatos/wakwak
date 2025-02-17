package com.social.login.filter;

import com.social.login.entity.User;
import com.social.login.provider.JWTProvider;
import com.social.login.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;
    private final JWTProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        log.info("🔹 [JwtAuthenticationFilter] 요청 시작: {}", request.getRequestURI());

        try {
            // 1️⃣ Authorization 헤더에서 JWT 토큰 추출
            String token = parseBearerToken(request);
            if (token == null) {
                log.warn("❌ [JwtAuthenticationFilter] 토큰 없음, 필터 통과");
                filterChain.doFilter(request, response);
                return;
            }
            log.info("🔹 [JwtAuthenticationFilter] 추출된 토큰: {}", token);

            // 2️⃣ JWT 검증 및 사용자 정보 추출
            Integer userId = jwtProvider.validateToken(token);
            if (userId == null) {
                log.warn("❌ [JwtAuthenticationFilter] JWT 검증 실패, 필터 통과");
                filterChain.doFilter(request, response);
                return;
            }
            log.info("✅ [JwtAuthenticationFilter] JWT 검증 성공 - 사용자 ID: {}", userId);

            // 3️⃣ DB에서 사용자 정보 조회
            Optional<User> optionalUser = userRepository.findByUserId(userId);
            User user = optionalUser.get();
            if (user == null) {
                log.warn("❌ [JwtAuthenticationFilter] 사용자 정보 없음: {}", userId);
                filterChain.doFilter(request, response);
                return;
            }
            log.info("✅ [JwtAuthenticationFilter] 사용자 조회 성공 - ID: {}, Role: {}", user.getUserId(), user.getUserRole());

            // 4️⃣ 권한 설정
            String role = user.getUserRole();
            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority(role));
            log.info("🔹 [JwtAuthenticationFilter] 부여된 권한: {}", role);

            // 5️⃣ SecurityContext 설정
            AbstractAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(user, null, authorities);
            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            log.info("✅ [JwtAuthenticationFilter] SecurityContext 설정 완료: {}", SecurityContextHolder.getContext().getAuthentication());

        } catch (Exception exception) {
            log.error("❌ [JwtAuthenticationFilter] 예외 발생: {}", exception.getMessage(), exception);
        }

        log.info("✅ [JwtAuthenticationFilter] 현재 SecurityContext 인증 객체: {}", SecurityContextHolder.getContext().getAuthentication());

        // 필터 체인 계속 실행
        filterChain.doFilter(request, response);
        log.info("🔹 [JwtAuthenticationFilter] 필터 체인 완료, 컨트롤러로 이동");
    }


    /**
     * Authorization 헤더에서 Bearer 토큰을 추출하는 메서드
     */
    private String parseBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");

        if (!StringUtils.hasText(authorization)) {
            log.warn("❌ [JwtAuthenticationFilter] Authorization 헤더 없음");
            return null;
        }

        if (!authorization.startsWith("Bearer ")) {
            log.warn("❌ [JwtAuthenticationFilter] Bearer 토큰 아님: {}", authorization);
            return null;
        }

        String token = authorization.substring(7);
        log.debug("🔹 [JwtAuthenticationFilter] 추출된 JWT: {}", token);
        return token;
    }
}
