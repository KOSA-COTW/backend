package cotw.server.domain.member.service;

import cotw.server.common.jwt.CustomUserDetails;
import cotw.server.domain.member.Dto.request.LoginRequestDTO;
import cotw.server.domain.member.Dto.request.SignUpRequestDTO;
import cotw.server.domain.member.Dto.response.ShowInfoResponseDTO;
import cotw.server.domain.member.Dto.response.SignUpResponseDTO;
import cotw.server.domain.member.entity.Member;
import cotw.server.domain.member.entity.Role;
import cotw.server.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;


    public SignUpResponseDTO signUpMember(SignUpRequestDTO signUpRequestDTO) {
        // 이메일 유무 확인
        memberRepository.findByEmail(signUpRequestDTO.email()).ifPresent(member -> {
                new IllegalArgumentException("Invalid email");
        });

        String encodedPassword = passwordEncoder.encode(signUpRequestDTO.password());
        Member newMember = signUpRequestDTO.toEntity(signUpRequestDTO.email(), encodedPassword, Role.USER);

        memberRepository.save(newMember);

        return SignUpResponseDTO.fromEntity(newMember);
    }

    public Member loginMember(LoginRequestDTO loginRequestDTO) {
        Member member = memberRepository.findByEmail(loginRequestDTO.email()).orElseThrow(()->
                new IllegalArgumentException("Invalid email"));

        if(!passwordEncoder.matches(loginRequestDTO.password(), member.getPassword())) {
            new IllegalArgumentException("Invalid password");
        }
        return member;
    }

    public void withdrawMember(CustomUserDetails customUserDetails) {
        Member member = memberRepository.findByEmail(customUserDetails.getUsername()).orElseThrow(
                () -> new IllegalArgumentException("Invalid email")
        );

        if(member != null) {
            memberRepository.delete(member);
        }
    }

    public ShowInfoResponseDTO showMemberInfo(CustomUserDetails customUserDetails) {
        Member member = memberRepository.findByEmail(customUserDetails.getUsername()).orElseThrow(
                () -> new IllegalArgumentException("Invalid member")
        );

        if(member == null) {
            throw new AccessDeniedException("Member not found");
        }

        ShowInfoResponseDTO responseDTO = ShowInfoResponseDTO.from(member);
        return responseDTO;
    }


}
