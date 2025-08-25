package ict.project.user.service;

import ict.project.user.UserEntity;
import ict.project.user.UserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.Keys;


@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final UserRepository userRepository;

    private final String secret = "yoakndjfksoqpongtkdnanqkdorfmalqrnricnamfkdqpefwjwdvnjdvnvdkddckadnfwfhwefvsjfofssds"; // 32바이트 이상
    private final long expiration = 1000 * 60 * 60; // 1시간

    private final Key key = Keys.hmacShaKeyFor(secret.getBytes());

    // JWT 생성
    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    //토큰에서 id 추출
    public Integer getUserId(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();

        UserEntity user = userRepository.findByEmail(claims.getSubject()).orElseThrow();
        return user.getId();
    }

    // JWT 검증
    public String validateToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();  // email 반환
        } catch (JwtException e) {
            return null;
        }
    }
}
