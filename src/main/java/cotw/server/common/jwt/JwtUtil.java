package cotw.server.common.jwt;


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
        System.out.println("Secret key length: " + secret.length());

    }

    public Boolean isExpired(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getExpiration().before(new Date());
    }

    public String getUsername(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("username", String.class);
    }

    public String getRole(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("role", String.class);
    }

    public String getCategory(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("category", String.class);
    }
    
    public Long getMemberId(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("memberId", Long.class);
    }

    public String createToken(String category, String username, String role, Long memberId, Long expirationTime) {

        return Jwts.builder()
                .claim("category", category)
                .claim("username", username)  //유저 명
                .claim("role", role)  //권한 정보
                .claim("memberId", memberId)  //회원 ID
                .setIssuedAt(new Date())    // 생성시간
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))  // 만료시간
                .signWith(secretKey)     // 서명 (HS512 알고리즘)
                .compact(); // 토큰 생성
    }
    
    // 기존 호환성을 위한 오버로드 메서드
    public String createToken(String category, String username, String role, Long expirationTime) {
        return createToken(category, username, role, null, expirationTime);
    }



}
