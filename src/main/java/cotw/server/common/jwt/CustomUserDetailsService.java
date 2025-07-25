package cotw.server.common.jwt;

import cotw.server.domain.member.entity.Member;
import cotw.server.domain.member.repository.MemberRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    public CustomUserDetailsService(MemberRepository memberRepository) {

        this.memberRepository = memberRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Member userData = memberRepository.findByEmail(username).orElseThrow(
                () -> new UsernameNotFoundException(username)
        );

        if (userData != null) {

            return new CustomUserDetails(userData);
        }


        return null;
    }
}
