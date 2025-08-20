package cotw.server.domain.payment.config;

import cotw.server.common.jwt.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    public static Long getCurrentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("인증되지 않은 사용자입니다.");
        }

        // CustomUserDetails에서 memberId를 가져오는 방식
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getMemberId();
        }
        
        throw new RuntimeException("인증 정보에서 회원 ID를 찾을 수 없습니다.");
    }
}
