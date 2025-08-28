package ict.project.user.service;

import ict.project.user.UserEntity;
import ict.project.user.UserRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final UserRepository userRepository;

    // 32바이트 이상
    private final String secret = "yoakndjfksoqpongtkdnanqkdorfmalqrnricnamfkdqpefwjwdvnjdvnvdkddckadnfwfhwefvsjfofssds";
    private final long expiration = 1000 * 60 * 60; // 1시간
    private final Key key = Keys.hmacShaKeyFor(secret.getBytes());

    /** ✅ JWT 생성: userId를 명시적으로 claim에 넣는다 */
    public String generateToken(UserEntity user) {
        return Jwts.builder()
                .setSubject(user.getEmail())            // sub = email (그대로 유지)
                .claim("userId", user.getId())          // ⭐ userId 클레임 추가
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /** (하위호환) 이메일만 받아오는 기존 메서드가 남아있다면 UserEntity로 위임 */
    @Deprecated
    public String generateToken(String email) {
        UserEntity user = userRepository.findByEmail(email).orElseThrow();
        return generateToken(user);
    }

    /** ✅ 토큰에서 userId 꺼내기 (없으면 email로 fallback) */
    public Integer getUserId(String token) {
        Claims claims = parseClaims(token);

        // 1) userId 클레임이 있으면 최우선 사용
        Object v = claims.get("userId");
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignore) {}
        }

        // 2) (구토큰 호환) subject(email)로 조회
        String email = claims.getSubject();
        UserEntity user = userRepository.findByEmail(email).orElseThrow();
        return user.getId();
    }

    /** 유효성 검사 – 유효하면 email(subject) 반환, 아니면 null */
    public String validateToken(String token) {
        try {
            return parseClaims(token).getSubject();
        } catch (JwtException e) {
            return null;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
