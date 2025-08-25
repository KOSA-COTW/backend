package cotw.server.common.jwt;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private SecretKey secretKey;

    public JwtUtil(@Value("${jwt.secret}")String secret) {

        this.secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),
                Jwts.SIG.HS512.key().build().getAlgorithm());

    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)      // 서명 검증 + 만료 검사
                    .getPayload();
        } catch (ExpiredJwtException e) {
            // 토큰이 만료되어도 Claims는 필요할 때가 있어 반환
            return e.getClaims();
        }
    }

    public Boolean isExpired(String token) {
        return parseClaims(token).getExpiration().before(new Date());
    }

    public String getUsername(String token) {
        return parseClaims(token).get("username", String.class);
    }

    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public String getCategory(String token) {
        return parseClaims(token).get("category", String.class);
    }
    
    public Long getMemberId(String token) {
        return parseClaims(token).get("memberId", Long.class);
    }

    public long getTokenVersion(String token) {
        Object v = parseClaims(token).get("tokenVersion");  // 여기서 사용
        if (v == null) return 0L;                // 과거 토큰 호환
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return 0L; }
    }

    public String createToken(String category, String username, String role, Long memberId, Long tokenVersion, Long expirationTime) {

        return Jwts.builder()
                .claim("category", category)
                .claim("username", username)  //유저 명
                .claim("role", role)  //권한 정보
                .claim("memberId", memberId)  //회원 ID
                .claim("tokenVersion", tokenVersion)    // token의 버전
                .setIssuedAt(new Date())    // 생성시간
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))  // 만료시간
                .signWith(secretKey)     // 서명 (HS512 알고리즘)
                .compact(); // 토큰 생성
    }
    
    // 기존 호환성을 위한 오버로드 메서드
    public String createToken(String category, String username, String role, Long expirationTime) {
        return createToken(category, username, role, null, null, expirationTime);
    }



}
