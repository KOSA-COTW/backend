package cotw.server.common.mail.service;

import cotw.server.common.jwt.service.RefreshTokenService;
import cotw.server.common.log.entity.AccountEventType;
import cotw.server.common.log.service.AccountAuditService;
import cotw.server.common.mail.dto.response.EmailCodeSendResponse;
import cotw.server.common.mail.dto.response.EmailCodeVerifyResponse;
import cotw.server.common.mail.dto.response.LookupResponse;
import cotw.server.common.mail.tokenPayload.EmailTokenPurpose;
import cotw.server.domain.member.dto.request.SignUpRequest;
import cotw.server.domain.member.entity.AccountStatus;
import cotw.server.domain.member.entity.Member;
import cotw.server.domain.member.entity.Role;
import cotw.server.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.time.Duration;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailAuthServiceImpl implements EmailAuthService {

    private final RateLimitService rateLimit;
    private final AccountAuditService audit;
    private final MemberRepository memberRepository;
    private final MailService mail;
    private final EmailTokenService emailTokenService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    private static final Duration TTL = Duration.ofMinutes(10);
    private static final Duration COOLDOWN = Duration.ofSeconds(60);

    private static final int VERIFY_ATTEMPTS_PER_MINUTE = 5;

    /* 아이디 찾기: 존재/검증된 이메일일 때만 발송. 외부 응답은 컨트롤러에서 항상 동일. */
    @Override
    @Transactional
    public LookupResponse sendUsernameLookupEmail(String email, String ip, String ua) {
        String norm = normalizeEmail(email);

        rateLimit.hitOrThrow("username-lookup:" + norm, 5, Duration.ofHours(1));
        rateLimit.hitOrThrow("username-lookup-ip:" + ip, 20, Duration.ofHours(1));

        boolean sent = false;
        LookupResponse response = null;
        Optional<Member> opt = memberRepository.findByVerifiedEmail(norm);
        if (opt.isPresent() && opt.get().getEmailVerifiedAt() != null) {
            Member m = opt.get();
            // 마스킹 대상은 "로그인 아이디/이메일" (요구사항에 맞춰 이메일 마스킹)
            String masked = maskUsername(m.getEmail());
            response = LookupResponse.from(masked);
            sent = true;
        }

        audit.log(AccountEventType.USERNAME_LOOKUP_REQUEST,
                opt.map(Member::getId).orElse(null), norm, ip, ua,
                "USERNAME_LOOKUP", sent, sent ? "sent" : "no-op");

        return response;
    }


    /* 회원가입 */
    @Override
    @Transactional
    public Long signup(SignUpRequest requestDTO, String ip, String ua) {
        String login = normalizeEmail(requestDTO.email());
        String verify = normalizeEmail(requestDTO.verifiedEmail());

        // 레이트리밋: 둘 다 묶어서 안전하게
        rateLimit.hitOrThrow("signup:" + login, 5, Duration.ofHours(1));
        rateLimit.hitOrThrow("email-verify:" + verify, 5, Duration.ofHours(1));

        if (memberRepository.existsByEmailIgnoreCase(login)) {
            throw new IllegalStateException("EMAIL_ALREADY_USED");
        }
        if (memberRepository.existsByVerifiedEmailIgnoreCase(verify)) {
            throw new IllegalStateException("VERIFY_EMAIL_ALREADY_USED");
        }

        int randomInt = (int) (Math.random() * 3) + 1;
        String pictureUrl = "/profile/profile"+ randomInt +".png";

        Member newMember = requestDTO.toEntity(requestDTO.name(), requestDTO.nickname(),
                requestDTO.email(), passwordEncoder.encode(requestDTO.password()),
                pictureUrl, requestDTO.verifiedEmail(), Role.USER);

        memberRepository.save(newMember);

        audit.log(AccountEventType.EMAIL_VERIFY_REQUEST, newMember.getId(), verify, ip, ua,
                "EMAIL_VERIFY", true, "signup_sent");

        return newMember.getId();
    }


    /* 미인증 사용자 대상 재전송
    * 아직 사용 안하는 메서드 */
    @Override
    @Transactional
    public void resendEmailVerification(String email, String ip, String ua) {
        String any = normalizeEmail(email);
        rateLimit.hitOrThrow("email-verify-resend-any:" + any, 3, Duration.ofHours(1));

        Optional<Member> opt = memberRepository.findByEmailOrVerifiedEmail(any, any);
        if (opt.isPresent()) {
            Member m = opt.get();
            if (m.getEmailVerifiedAt() != null) {
                audit.log(AccountEventType.EMAIL_VERIFY_REQUEST, m.getId(), any, ip, ua,
                        "EMAIL_VERIFY", false, "already_verified");
                return;
            }
            String verify = m.getVerifiedEmail();
            if (verify == null || verify.isBlank()) {
                audit.log(AccountEventType.EMAIL_VERIFY_REQUEST, m.getId(), any, ip, ua,
                        "EMAIL_VERIFY", false, "no_verified_email");
                return;
            }

            rateLimit.hitOrThrow("email-verify-resend-target:" + verify, 3, Duration.ofHours(1));

            String token = emailTokenService.create(
                    m.getId(), verify, EmailTokenPurpose.EMAIL_VERIFY, Duration.ofMinutes(15), ip, ua);

            String link = "http://localhost:5173/verify-email?token=" + token
                    + "&email=" + URLEncoder.encode(verify, java.nio.charset.StandardCharsets.UTF_8);

            mail.sendHtml(verify, "[COTW] 이메일 인증 다시 보내기",
                    "<p>아래 버튼을 눌러 이메일 인증을 완료하세요.</p><p><a href='" + link + "'>인증하기</a></p>");
            audit.log(AccountEventType.EMAIL_VERIFY_REQUEST, m.getId(), verify, ip, ua,
                    "EMAIL_VERIFY", true, "resend");
        } else {
            audit.log(AccountEventType.EMAIL_VERIFY_REQUEST, null, any, ip, ua,
                    "EMAIL_VERIFY", false, "no-op");
        }
    }

    @Override
    public EmailCodeSendResponse sendCode(String rawEmail, EmailTokenPurpose purpose, String ip) {
        // 1) 정규화
        if (rawEmail == null || rawEmail.isBlank()) {
            return EmailCodeSendResponse.fail("INVALID", "이메일이 비어 있습니다.", (int) COOLDOWN.getSeconds());
        }
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);

        // 2) 레이트리밋 (이메일 + IP 기반 키)
        String keyEmail = "email:code:send:" + purpose + ":" + email;
        String keyIp    = "email:code:send:ip:" + ip;
        try {
            // 60초에 1회 정도로 제한 (필요 시 maxHits 조절)
            rateLimit.checkOrThrow(keyEmail, COOLDOWN, 1);
            rateLimit.checkOrThrow(keyIp, COOLDOWN, 3); // IP당 60초 3회 같은 세이프가드
        } catch (Exception ex) {
            return EmailCodeSendResponse.fail("RATE_LIMIT", "요청이 너무 잦습니다. 잠시 후 다시 시도해 주세요.", (int) COOLDOWN.getSeconds());
        }

        // 3) 목적별 사전검증
        switch (purpose) {
            case SIGNUP -> {
                // 회원가입: 이미 존재하면 보내지 않음(프론트도 dup-check 하지만 서버에서 2차 방어)
                if (memberRepository.existsByEmailIgnoreCase(email)) {
                    return EmailCodeSendResponse.fail("DUPLICATE_EMAIL", "이미 가입된 이메일입니다.", (int) COOLDOWN.getSeconds());
                }
            }
            case PASSWORD_RESET, USERNAME_LOOKUP -> {
                // 비번재설정/아이디찾기: 존재하는 이메일만 OK
                Member member = memberRepository.findByVerifiedEmail(email).orElse(null);
                if (member == null) {
                    // 존재여부를 숨기고 싶다면 OK로 돌려도 됨(메일만 조건부로 발송)
                    return EmailCodeSendResponse.fail("NOT_FOUND", "가입 이력이 없는 이메일입니다.", (int) COOLDOWN.getSeconds());
                }
                if (member.getStatus().equals(AccountStatus.DELETED)) {
                    return EmailCodeSendResponse.fail("ACCOUNT_DELETED", "삭제된 이메일입니다.", (int) COOLDOWN.getSeconds());
                }
            }
            case ACCOUNT_RECOVER -> {
                Member member = memberRepository.findByEmail(email).orElse(null);
                if(member == null) {
                    member = memberRepository.findByVerifiedEmail(email).orElse(null);
                }
                if(member != null && !member.getStatus().equals(AccountStatus.DELETED)) {
                    return EmailCodeSendResponse.fail("NOT_DELETED", "삭제되지않은 이메일입니다.", (int) COOLDOWN.getSeconds());
                }
                email = member.getVerifiedEmail();
            }
            default -> { /* 다른 목적 추가 시 여기에 정책 삽입 */ }
        }

        // 4) 코드 생성 & 저장
        String code = generate6DigitCode();
        emailTokenService.save(email, purpose, code, TTL);

        // 5) 메일 발송
        String subject = subjectFor(purpose);
        String body = """
                안녕하세요.
                아래 인증코드를 입력해 주세요.

                인증코드: %s
                유효시간: %d분

                본 메일은 발신 전용입니다.
                """.formatted(code, TTL.toMinutes());
        try {
            mail.sendText(email, subject, body);
        } catch (Exception e) {
            return EmailCodeSendResponse.fail("MAIL_SEND_FAILED", "메일 발송에 실패했습니다.", (int) COOLDOWN.getSeconds());
        }

        // 6) 응답
        return EmailCodeSendResponse.ok((int) COOLDOWN.getSeconds());
    }

    @Override
    public EmailCodeVerifyResponse verifyCode(String rawEmail, EmailTokenPurpose purpose, String rawCode, String ip) {
        if (rawEmail == null || rawCode == null) {
            return EmailCodeVerifyResponse.fail("INVALID", "요청 값이 올바르지 않습니다.");
        }

        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        String code  = rawCode.trim();

        // 1) 레이트리밋: 시도 남용 방지
        String key = "email:code:verify:" + purpose + ":" + email;
        String keyIp = "email:code:verify:ip:" + ip;
        try {
            rateLimit.checkOrThrow(key,   Duration.ofMinutes(1), VERIFY_ATTEMPTS_PER_MINUTE);
            rateLimit.checkOrThrow(keyIp, Duration.ofMinutes(1), VERIFY_ATTEMPTS_PER_MINUTE * 2);
        } catch (Exception ex) {
            return EmailCodeVerifyResponse.fail("RATE_LIMIT", "시도 횟수가 너무 많습니다. 잠시 후 다시 시도해 주세요.");
        }

        // 2) 목적별 사전 검증(선택)
        switch (purpose) {
            case SIGNUP -> {
                // 가입용: 이미 존재 이메일이면 굳이 검증 진행하지 않음(정책에 따라 다르게)
                if (memberRepository.existsByEmailIgnoreCase(email)) {
                    return EmailCodeVerifyResponse.fail("DUPLICATE_EMAIL", "이미 가입된 이메일입니다.");
                }
            }
            case PASSWORD_RESET, USERNAME_LOOKUP -> {
                // 비번재설정/아이디찾기: 존재하는 이메일만 OK
                Member member = memberRepository.findByVerifiedEmail(email).orElse(null);
                if (member == null) {
                    // 존재여부를 숨기고 싶다면 OK로 돌려도 됨(메일만 조건부로 발송)
                    return EmailCodeVerifyResponse.fail("NOT_FOUND", "가입 이력이 없는 이메일입니다.");
                }
                if (member.getStatus().equals(AccountStatus.DELETED)) {
                    return EmailCodeVerifyResponse.fail("ACCOUNT_DELETED", "삭제된 이메일입니다.");
                }
            }
            case ACCOUNT_RECOVER -> {
                Member member = memberRepository.findByEmail(email).orElse(null);
                if(member == null) {
                    member = memberRepository.findByVerifiedEmail(email).orElse(null);
                }
                if(member != null && !member.getStatus().equals(AccountStatus.DELETED)) {
                    return EmailCodeVerifyResponse.fail("NOT_DELETED", "삭제되지않은 이메일입니다.");
                }
                email = member.getVerifiedEmail();
            }
            default -> { /* 추가 목적 시 정책 삽입 */ }
        }

        // 3) 코드 검증 & 1회성 소비
        EmailTokenService.VerifyResult result = emailTokenService.verifyAndConsume(email, purpose, code);
        return switch (result) {
            case OK            -> EmailCodeVerifyResponse.ok();
            case MISMATCH      -> EmailCodeVerifyResponse.fail("MISMATCH", "인증코드가 일치하지 않습니다.");
            case EXPIRED       -> EmailCodeVerifyResponse.fail("EXPIRED", "인증코드가 만료되었거나 존재하지 않습니다.");
            case INTERNAL_ERROR-> EmailCodeVerifyResponse.fail("ERROR", "검증 중 오류가 발생했습니다.");
        };
    }

    @Override
    @Transactional
    public void resetPassword(String email, String newRawPassword) {
        Member m = memberRepository.findByVerifiedEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("NOT_FOUND"));
        String encoded = passwordEncoder.encode(newRawPassword);
        m.setPassword(encoded);
        // 토큰 무효화(선택)
        try {
            refreshTokenService.revokeAllByUser(email);
        } catch (Exception ignore) {
            log.debug("refresh token revoke skipped: {}", ignore.getMessage());
        }
        // JPA 영속 상태면 save 불필요. 비영속이면 repository.save(m);
        memberRepository.save(m);
    }

    /* ===== 유틸 ===== */

    private String generate6DigitCode() {
        int n = ThreadLocalRandom.current().nextInt(0, 1_000_000);
        return String.format("%06d", n);
    }

    private String subjectFor(EmailTokenPurpose purpose) {
        return switch (purpose) {
            case SIGNUP -> "[COTW] 회원가입 이메일 인증코드";
            case PASSWORD_RESET -> "[COTW] 비밀번호 재설정 인증코드";
            case USERNAME_LOOKUP -> "[COTW] 아이디 찾기 인증코드";
            case ACCOUNT_RECOVER -> "[COTW] 계정 복구 인증코드";
            default -> "[COTW] 이메일 인증코드";
        };
    }


    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private String maskUsername(String u) {
        if (u == null || u.isEmpty()) return "***";
        if (u.contains("@")) {
            int at = u.indexOf('@');
            String local = u.substring(0, at);
            String domain = u.substring(at + 1);

            int localHead = Math.min(4, local.length());
            String localMasked = local.substring(0, localHead)
                    + repeat('*', Math.max(local.length() - localHead, 0));

            int domainTail = Math.min(6, domain.length());
            String domainMasked = repeat('*', Math.max(domain.length() - domainTail, 0))
                    + (domainTail > 0 ? domain.substring(domain.length() - domainTail) : "");

            return localMasked + "@" + domainMasked;
        }
        int head = Math.min(3, u.length());
        int tail = Math.min(3, u.length() - head);
        return u.substring(0, head) + repeat('*', Math.max(u.length() - head - tail, 0))
                + (tail > 0 ? u.substring(u.length() - tail) : "");
    }

    private static String repeat(char c, int n) {
        if (n <= 0) return "";
        char[] arr = new char[n];
        Arrays.fill(arr, c);
        return new String(arr);
    }
}
