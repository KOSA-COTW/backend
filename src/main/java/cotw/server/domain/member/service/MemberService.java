package cotw.server.domain.member.service;

import cotw.server.common.jwt.CustomUserDetails;
import cotw.server.common.jwt.service.RefreshTokenService;
import cotw.server.domain.board.repository.PostRepository;
import cotw.server.domain.board.service.PostService;
import cotw.server.domain.comment.repository.CommentLikeRepository;
import cotw.server.domain.comment.repository.CommentReportRepository;
import cotw.server.domain.comment.repository.CommentRepository;
import cotw.server.domain.member.dto.response.DupCheckResponse;
import cotw.server.domain.member.dto.response.ShowInfoResponse;
import cotw.server.domain.member.entity.AccountStatus;
import cotw.server.domain.member.entity.Member;
import cotw.server.domain.member.repository.MemberEmailProjection;
import cotw.server.domain.member.repository.MemberRepository;
import cotw.server.domain.payment.entity.PaymentLedger;
import cotw.server.domain.payment.entity.PaymentStatus;
import cotw.server.domain.payment.repository.PaymentLedgerRepository;

import cotw.server.domain.payment.repository.PaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final PaymentLedgerRepository paymentLedgerRepository;  // 결제 내역 레포지토리

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final CommentReportRepository commentReportRepository;
    private final PostRepository postRepository;
    private final PostService postService;
    private final PaymentOrderRepository paymentOrderRepository;

    // 간단 이메일/닉네임 검증(프론트 검증과 별개로 백엔드에서도 가볍게 방어)
    private static final Pattern EMAIL_RX = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern NICK_RX  = Pattern.compile("^[A-Za-z0-9가-힣._-]{2,20}$");

    // 예약/금지 닉네임(선택)
    private static final Set<String> RESERVED_NICKS = Set.of("admin","administrator","owner","system","null","undefined","root");


    public boolean validatePassword(Long memberId, String password) {
        Member m = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("member not found"));
        if (!passwordEncoder.matches(password, m.getPassword()))
            throw new AccessDeniedException("invalid password");
        return true;
    }


    public ShowInfoResponse showMemberInfo(CustomUserDetails customUserDetails) {
        Member member = memberRepository.findByEmail(customUserDetails.getUsername()).orElseThrow(
                () -> new IllegalArgumentException("Invalid member")
        );

        List<PaymentLedger> payments = paymentLedgerRepository.findByMemberIdAndStatus(customUserDetails.getMemberId(), PaymentStatus.DONE);

        int oneTimeCount = payments.size();
        Long totalDonation = payments.stream().mapToLong(PaymentLedger::getAmount).sum();


        ShowInfoResponse responseDTO = ShowInfoResponse.from(member, oneTimeCount, totalDonation);
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
        Member m = memberRepository.findByEmail(email).orElse(null);
        if(m == null) m = memberRepository.findByVerifiedEmail(email).orElseThrow(
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

    // 스케쥴러를 통해 작동되는 메서드. 소프트 삭제 상태이고 보관 만료된 계정들 삭제
    public int hardDeleteExpiredMembersChunk(int chunkSize) {
        List<Long> ids = memberRepository.findIdsToHardDelete(LocalDateTime.now(),
                (Pageable) PageRequest.of(0, chunkSize));
        if (ids.isEmpty()) return 0;

        // 0) 대체 사용자(탈퇴 사용자) 참조
        final Long DELETED_USER_ID = 1L; // 운영에서 실제 ID로 설정
        Member deletedUser = memberRepository.getReferenceById(DELETED_USER_ID);

        // 1) 행위 엔티티 정리(예: 좋아요/신고) — 벌크 삭제 메서드 필요
        commentLikeRepository.deleteByMemberIdIn(ids);
        commentReportRepository.deleteByMemberIdIn(ids);

        // 2) 글/댓글은 익명화(권장) 또는 삭제 중 정책 선택
        postRepository.anonymizeAuthorByMemberIds(ids, deletedUser);
        commentRepository.anonymizeAuthorByMemberIds(ids, deletedUser);

        // 2-1) 결제 주문도 FK 치환
        paymentOrderRepository.reassignMemberToDeleted(ids, deletedUser);

        // (선택) Participant 등 다른 FK도 정책에 따라 삭제/치환
        // participantRepository.deleteByMemberIdIn(ids);
        // 또는 participantRepository.reassignMemberToDeleted(ids, deletedUser);

        // 3) refresh 전부 제거
        List<MemberEmailProjection> emails = memberRepository.findEmailsByIdIn(ids);
        for (MemberEmailProjection p : emails) {
            // (a) 기존 DB/외부 저장소에 있는 refresh 토큰 정리
            refreshTokenService.revokeAllByUser(p.getEmail());
        }

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

    public void editImage(CustomUserDetails customUserDetails, MultipartFile file) {
        Member member = memberRepository.findByEmail(customUserDetails.getUsername()).orElseThrow(
                () -> new IllegalArgumentException("user not found")
        );
        if(member == null) throw new AccessDeniedException("member not found");

        String imageUrl = null;
        try {
            imageUrl = postService.upload(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        member.setPictureUrl(imageUrl);
        memberRepository.save(member);
    }

    public void editNickname(CustomUserDetails customUserDetails, String newNickname) {
        Member member = memberRepository.findByEmail(customUserDetails.getUsername()).orElseThrow(
                () -> new IllegalArgumentException("user not found")
        );
        if(member == null) throw new AccessDeniedException("member not found");

        member.setNickname(newNickname);
        memberRepository.save(member);
    }


    public DupCheckResponse checkEmail(String rawEmail) {
        if (rawEmail == null) return DupCheckResponse.invalid("email");
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        if (email.isEmpty() || !EMAIL_RX.matcher(email).matches()) {
            return DupCheckResponse.invalid("email");
        }
        boolean exists = memberRepository.existsByEmailIgnoreCase(email);
        return exists ? DupCheckResponse.dup("email") : DupCheckResponse.ok();
    }

    public DupCheckResponse checkNickname(String rawNickname) {
        if (rawNickname == null) return DupCheckResponse.invalid("nickname");
        String nick = rawNickname.trim();
        if (nick.isEmpty() || !NICK_RX.matcher(nick).matches()) {
            return DupCheckResponse.invalid("nickname");
        }
        if (RESERVED_NICKS.contains(nick.toLowerCase(Locale.ROOT))) {
            return new DupCheckResponse(false, "RESERVED", "This nickname is reserved");
        }
        boolean exists = memberRepository.existsByNicknameIgnoreCase(nick);
        return exists ? DupCheckResponse.dup("nickname") : DupCheckResponse.ok();
    }
}
