package cotw.server.domain.member.service;

import cotw.server.common.jwt.CustomUserDetails;
import cotw.server.common.jwt.service.RefreshTokenService;
import cotw.server.domain.member.Dto.request.LoginRequestDTO;
import cotw.server.domain.member.Dto.request.SignUpRequestDTO;
import cotw.server.domain.member.Dto.response.ShowInfoResponseDTO;
import cotw.server.domain.member.Dto.response.SignUpResponseDTO;
import cotw.server.domain.member.entity.AccountStatus;
import cotw.server.domain.member.entity.Member;
import cotw.server.domain.member.entity.Role;
import cotw.server.domain.member.repository.MemberRepository;
import cotw.server.domain.payment.dto.response.PaymentHistoryResponse;
import cotw.server.domain.payment.entity.PaymentStatus;
import cotw.server.domain.payment.repository.PaymentOrderRepository;
import cotw.server.domain.payment.repository.PaymentRepository;
import cotw.server.domain.payment.service.PaymentService;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final PaymentService paymentService;
    private final PaymentOrderRepository paymentOrderRepository;



    public SignUpResponseDTO signUpMember(SignUpRequestDTO signUpRequestDTO) {
        // 이메일 유무 확인
        if (memberRepository.findByEmail(signUpRequestDTO.email()).isPresent())
            throw new IllegalArgumentException("email already used");

        String encodedPassword = passwordEncoder.encode(signUpRequestDTO.password());
        Member newMember = signUpRequestDTO.toEntity(signUpRequestDTO.name(), signUpRequestDTO.nickname(), signUpRequestDTO.email(), encodedPassword, Role.USER);

        memberRepository.save(newMember);

        return SignUpResponseDTO.fromEntity(newMember);
    }

    public boolean validatePassword(Long memberId, String password) {
        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("member not found"));
        if (!passwordEncoder.matches(password, m.getPassword()))
            throw new AccessDeniedException("invalid password");
        return true;
    }


    public ShowInfoResponseDTO showMemberInfo(CustomUserDetails customUserDetails) {
        Member member = memberRepository.findByEmail(customUserDetails.getUsername()).orElseThrow(
                () -> new IllegalArgumentException("Invalid member")
        );

        List<PaymentHistoryResponse> payments = paymentOrderRepository.findByMemberIdAndStatus(customUserDetails.getMemberId(), PaymentStatus.DONE);

        int oneTimeCount = payments.size();
        Long totalDonation = payments.stream().mapToLong(PaymentHistoryResponse::getAmount).sum();

        ShowInfoResponseDTO responseDTO = ShowInfoResponseDTO.from(member, oneTimeCount, totalDonation);
        return responseDTO;
    }


    @Transactional
    public void deactivate(Long memberId, Duration retention, String password) {
        Member m = memberRepository.getReferenceById(memberId);
        boolean valid = validatePassword(memberId, password);
        if(valid){
            m.setStatus(AccountStatus.DELETED);
            m.setDeletedAt(LocalDateTime.now());
            m.setRetentionUntil(LocalDateTime.now().plus(retention));
            m.setTokenVersion(m.getTokenVersion() + 1); // 기존 토큰 무효화
            refreshTokenService.revokeAllByUser(m.getEmail()); // Refresh 즉시 폐기
        }else {
            throw new AccessDeniedException("Invalid member");
        }
    }

    @Transactional
    public void recover(String email) {
        Member m = memberRepository.findByEmail(email).orElseThrow(
                () -> new IllegalArgumentException("user not found")
        );
        if (m.getStatus() != AccountStatus.DELETED) throw new IllegalStateException("not deleted");
        if (m.getRetentionUntil() != null && m.getRetentionUntil().isBefore(LocalDateTime.now()))
            throw new IllegalStateException("retention expired");

        m.setStatus(AccountStatus.ACTIVE);
        m.setDeletedAt(null);
        m.setRetentionUntil(null);
        m.setTokenVersion(m.getTokenVersion() + 1); // 새 버전으로 재발급 유도

        // 방어적으로 기존 refresh 전부 제거 (깨끗한 상태로 시작)
        refreshTokenService.revokeAllByUser(m.getEmail());
    }

    public int hardDeleteExpiredMembersChunk(int chunkSize) {
        List<Long> ids = memberRepository.findIdsToHardDelete(LocalDateTime.now(),
                (Pageable) PageRequest.of(0, chunkSize));
        if (ids.isEmpty()) return 0;

        // 1) 행위 엔티티 정리(예: 좋아요/신고) — 벌크 삭제 메서드 필요
        // likeRepo.deleteByMemberIdIn(ids);
        // reportRepo.deleteByMemberIdIn(ids);

        // 2) 글/댓글은 익명화(권장) 또는 삭제 중 정책 선택
        // postRepo.anonymizeAuthorByMemberIds(ids, DELETED_USER_ID);
        // commentRepo.anonymizeAuthorByMemberIds(ids, DELETED_USER_ID);

        // 3) refresh 전부 제거
        // for (Long id : ids) { refreshTokenService.revokeAllByUser(findEmailById(id)); } // 구현 방식에 맞게 적용

        // 4) 마지막으로 회원 삭제
        memberRepository.deleteByIdIn(ids);
        return ids.size();
    }

    @Transactional
    public void editPassword(CustomUserDetails customUserDetails, String password, String newPassword) {
        Member member = memberRepository.findByEmail(customUserDetails.getUsername()).orElseThrow(  );
        boolean valid = validatePassword(member.getId(), password);
        if(!valid){
            throw new AccessDeniedException("invalid password");
        }else {
            member.setPassword(passwordEncoder.encode(newPassword));
            // 비번 변경 시 모든 토큰 무효화 + 재발급 유도
            member.setTokenVersion(member.getTokenVersion() + 1);
            memberRepository.save(member);
            refreshTokenService.revokeAllByUser(member.getEmail());
        }
    }

    public void editImage(CustomUserDetails customUserDetails, String imageUrl) {
        Member member = memberRepository.findByEmail(customUserDetails.getUsername()).orElseThrow(  );
        if(member == null) throw new AccessDeniedException("member not found");

        member.setPictureUrl(imageUrl);
        memberRepository.save(member);
    }

    public void editNickname(CustomUserDetails customUserDetails, String newNickname) {
        Member member = memberRepository.findByEmail(customUserDetails.getUsername()).orElseThrow(  );
        if(member == null) throw new AccessDeniedException("member not found");

        member.setNickname(newNickname);
        memberRepository.save(member);
    }


}
