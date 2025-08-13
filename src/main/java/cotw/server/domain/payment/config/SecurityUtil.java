package cotw.server.domain.payment.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    public static Long getCurrentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("인증되지 않은 사용자입니다.");
        }

        // JWT에서 추출한 회원 ID를 반환 (실제 구현에 따라 달라질 수 있음)
        // 예: CustomUserDetails에서 memberId를 가져오는 방식
        return Long.valueOf(authentication.getName());
    }
}
