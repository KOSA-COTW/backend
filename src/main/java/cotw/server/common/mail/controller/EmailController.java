package cotw.server.common.mail.controller;

import cotw.server.common.mail.dto.request.*;
import cotw.server.common.mail.dto.response.*;
import cotw.server.common.mail.service.EmailAuthService;
import cotw.server.domain.member.dto.request.SignUpRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("api/auth")
@RequiredArgsConstructor
public class EmailController {

    private final EmailAuthService emailAuthService;

    @PostMapping("/username/lookup")
    public ResponseEntity<LookupResponse> lookup(@RequestBody @Valid LookUpReqeust req,
                                                 HttpServletRequest http) {
        LookupResponse response = emailAuthService.sendUsernameLookupEmail(req.email(), http.getRemoteAddr(), http.getHeader("User-Agent"));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/signup")
    public ResponseEntity<SignupResponse> signup(@RequestBody @Valid SignUpRequest req,
                                                    HttpServletRequest http) {
        Long memberId = emailAuthService.signup(
                req,
                http.getRemoteAddr(), http.getHeader("User-Agent")
        );
        return ResponseEntity.status(201).body(new SignupResponse(memberId, "가입을 완료하려면 이메일 인증을 진행해 주세요."));
    }

    @PostMapping("/email/verify/resend")
    public ResponseEntity<ApiMessage> resend(@RequestBody @Valid ResendEmailVerifyRequest req,
                                             HttpServletRequest http) {
        emailAuthService.resendEmailVerification(req.email(), http.getRemoteAddr(), http.getHeader("User-Agent"));
        return ResponseEntity.ok(new ApiMessage("인증 메일을 전송했어요. 메일함을 확인해 주세요."));
    }

    @PostMapping("/email/send")
    public ResponseEntity<EmailCodeSendResponse> send(@Valid @RequestBody EmailCodeSendRequest req,
                                                      HttpServletRequest httpReq) {
        String ip = clientIp(httpReq);
        EmailCodeSendResponse res = emailAuthService.sendCode(req.email(), req.purpose(), ip);
        // 분기 처리: 실패한 경우와 성공한 경우를 구분
        if (res.sent()) {
            return ResponseEntity.ok(res); // 성공적인 코드 발송
        } else {
            return ResponseEntity.status(400).body(res); // 실패한 경우 (예: 레이트 리미트 초과, 이메일 존재하지 않음 등)
        }
    }

    @PostMapping("/email/verify")
    public ResponseEntity<EmailCodeVerifyResponse> verify(@Valid @RequestBody EmailCodeVerifyRequest req,
                                                          HttpServletRequest httpReq) {
        String ip = clientIp(httpReq);
        EmailCodeVerifyResponse res = emailAuthService.verifyCode(
                req.email(), req.purpose(), req.code(), ip
        );
        // 분기 처리: 인증 실패와 성공을 구분
        if (res.verified() && res.codeType().equals("OK")) {
            return ResponseEntity.ok(res); // 인증 성공
        } else if (res.verified() && res.codeType().equals("MISMATCH")) {
            return ResponseEntity.status(400).body(res); // 인증 코드 불일치
        } else if (res.verified() && res.codeType().equals("EXPIRED")) {
            return ResponseEntity.status(400).body(res); // 인증 코드 만료
        } else {
            return ResponseEntity.status(500).body(res); // 기타 오류 처리
        }
    }

    // 비로그인: 이메일+코드로 비밀번호 재설정 (프론트: POST 사용)
    @PostMapping("/password/reset")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordResetRequest req) {

        emailAuthService.resetPassword(req.email(), req.newPassword());

        return ResponseEntity.ok(Map.of("status", "OK"));
    }


    private String clientIp(HttpServletRequest request) {
        String h = request.getHeader("X-Forwarded-For");
        if (h != null && !h.isBlank()) return h.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}
