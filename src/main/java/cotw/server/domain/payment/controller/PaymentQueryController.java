package cotw.server.domain.payment.controller;

import cotw.server.domain.payment.config.SecurityUtil;
import cotw.server.domain.payment.dto.response.PaymentHistoryResponse;
import cotw.server.domain.payment.service.LedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments/queries")
@RequiredArgsConstructor
public class PaymentQueryController {

    private final LedgerService ledgerService;

    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<PaymentHistoryResponse>> getPaymentsByMember(@PathVariable Long memberId) {
        List<PaymentHistoryResponse> responses = ledgerService.getPaymentHistoryByMember(memberId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<PaymentHistoryResponse>> getPaymentsByPost(@PathVariable Long postId) {
        List<PaymentHistoryResponse> responses = ledgerService.getPaymentHistoryByPost(postId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/my")
    public ResponseEntity<List<PaymentHistoryResponse>> getMyPayments() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        List<PaymentHistoryResponse> responses = ledgerService.getPaymentHistoryByMember(memberId);
        return ResponseEntity.ok(responses);
    }
}