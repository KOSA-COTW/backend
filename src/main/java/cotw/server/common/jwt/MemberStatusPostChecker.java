package cotw.server.common.jwt;

import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.stereotype.Component;

// 상태에 따라 서로 다른 예외를 던짐 (비번 검증 "통과 후" 호출됨)
@Component
public class MemberStatusPostChecker implements UserDetailsChecker {

    @Override
    public void check(UserDetails user) {
        var cud = (CustomUserDetails) user;
        var m = cud.getMember();

        switch (m.getStatus()) {
            case ACTIVE -> { /* OK */ }
            case DELETED -> throw new DisabledException("ACCOUNT_DELETED");
            case SUSPENDED -> throw new LockedException("ACCOUNT_SUSPENDED");
            case PENDING -> throw new AccountExpiredException("ACCOUNT_PENDING");
            default -> throw new AuthenticationException("ACCOUNT_INVALID") {};
        }
    }
}
