package cotw.server.domain.payment.controller;

import cotw.server.domain.payment.config.SecurityUtil;
import cotw.server.domain.payment.dto.request.PaymentCancelRequest;
import cotw.server.domain.payment.dto.request.PaymentConfirmRequest;
import cotw.server.domain.payment.dto.request.PaymentCreateRequest;
import cotw.server.domain.payment.dto.response.PaymentCancelResponse;
import cotw.server.domain.payment.dto.response.PaymentCreateResponse;
import cotw.server.domain.payment.dto.response.PaymentDetailResponse;
import cotw.server.domain.payment.service.PaymentService;
import cotw.server.domain.payment.service.LedgerService;
import cotw.server.domain.payment.entity.PaymentLedger;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final LedgerService ledgerService;

    @PostMapping
    public ResponseEntity<PaymentCreateResponse> createPayment(@RequestBody PaymentCreateRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId(); // JWT에서 회원 ID 추출
        PaymentCreateResponse response = paymentService.createPayment(request, memberId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/success")
    public ResponseEntity<String> paymentSuccess(
            @RequestParam String orderId,
            @RequestParam String paymentKey,
            @RequestParam Integer amount) {

        PaymentConfirmRequest confirmRequest = PaymentConfirmRequest.builder()
                .paymentKey(paymentKey)
                .orderId(orderId)
                .amount(amount)
                .build();

        try {
            paymentService.confirmPayment(confirmRequest);
            return ResponseEntity.status(302)
                    .header("Location", "http://localhost:5173/payment/success?orderId=" + orderId)
                    .build();
        } catch (Exception e) {
            return ResponseEntity.status(302)
                    .header("Location", "http://localhost:5173/payment/fail?error=payment_failed")
                    .build();
        }
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<PaymentLedger>> getPaymentsByMember(@PathVariable Long memberId) {
        List<PaymentLedger> responses = ledgerService.getPaymentLedgersByMember(memberId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<PaymentLedger>> getPaymentsByPost(@PathVariable Long postId) {
        List<PaymentLedger> responses = ledgerService.getPaymentLedgersByPost(postId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/my")
    public ResponseEntity<List<PaymentLedger>> getMyPayments() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        List<PaymentLedger> responses = ledgerService.getPaymentLedgersByMember(memberId);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/cancel")
    public ResponseEntity<PaymentCancelResponse> cancelPayment(@RequestBody PaymentCancelRequest request) {
        PaymentCancelResponse response = paymentService.cancelPayment(request);
        return ResponseEntity.ok(response);
    }
}
