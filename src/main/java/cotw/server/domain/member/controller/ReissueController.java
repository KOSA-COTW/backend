package cotw.server.domain.member.controller;

import cotw.server.common.jwt.JwtUtil;
import cotw.server.common.jwt.entity.RefreshToken;
import cotw.server.common.jwt.repository.RefreshTokenRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Date;

@Controller
@ResponseBody
@RequiredArgsConstructor
public class ReissueController {

    private final JwtUtil jwtUtil;

    private final RefreshTokenRepository refreshTokenRepository;

    @PostMapping("/reissue")
    public ResponseEntity<?> reissue(HttpServletRequest request, HttpServletResponse response) {

        //get refresh token
        String refresh = null;
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {

            if (cookie.getName().equals("refresh")) {

                refresh = cookie.getValue();
            }
        }

        if (refresh == null) {

            //response status code
            return new ResponseEntity<>("refresh token null", HttpStatus.BAD_REQUEST);
        }

        //expired check
        try {
            jwtUtil.isExpired(refresh);
        } catch (ExpiredJwtException e) {

            //response status code
            return new ResponseEntity<>("refresh token expired", HttpStatus.BAD_REQUEST);
        }

        // 토큰이 refresh인지 확인 (발급시 페이로드에 명시)
        String category = jwtUtil.getCategory(refresh);

        if (!category.equals("refresh")) {

            //response status code
            return new ResponseEntity<>("invalid refresh token", HttpStatus.BAD_REQUEST);
        }

        // DB에 저장됐는지 확인
        Boolean isExist = refreshTokenRepository.existsByRefreshToken(refresh);
        if (!isExist) {
            // response body
            return new ResponseEntity<>("refresh token not found", HttpStatus.BAD_REQUEST);
        }

        String username = jwtUtil.getUsername(refresh);
        String role = jwtUtil.getRole(refresh);
        Long memberId = jwtUtil.getMemberId(refresh);

        //make new JWT
        String newAccess = jwtUtil.createToken("access", username, role, memberId, 1000*60*60L);  // 1시간
        String newRefresh = jwtUtil.createToken("refresh", username, role, memberId, 1000*60*60*24L);

        // Refresh token 저장. DB에 기존 Refresh token 삭제 후 새 Refresh token 저장
        refreshTokenRepository.deleteByRefreshToken(refresh);
        addRefreshEntity(username, newRefresh, 1000*60*60*24L);


        //response
        response.setHeader("access", newAccess);
        response.addCookie(createCookie("refresh", newRefresh));

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private void addRefreshEntity(String username, String newRefresh, Long expiry) {

        Date date = new Date(System.currentTimeMillis() + expiry);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setEmail(username);
        refreshToken.setRefreshToken(newRefresh);
        refreshToken.setExpiryDate(date.toString());

        refreshTokenRepository.save(refreshToken);
    }

    private Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24 * 60 * 60); // 24시간
        cookie.setSecure(true);
        return cookie;
    }

}
