package cotw.server.domain.payment.controller;

import cotw.server.domain.payment.config.SecurityUtil;
import cotw.server.domain.payment.dto.request.PaymentCancelRequest;
import cotw.server.domain.payment.dto.request.PaymentConfirmRequest;
import cotw.server.domain.payment.dto.request.PaymentCreateRequest;
import cotw.server.domain.payment.dto.response.PaymentCancelResponse;
import cotw.server.domain.payment.dto.response.PaymentConfirmResponse;
import cotw.server.domain.payment.dto.response.PaymentCreateResponse;
import cotw.server.domain.payment.dto.response.PaymentDetailResponse;
import cotw.server.domain.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentCreateResponse> createPayment(@RequestBody PaymentCreateRequest request) {
        Long memberId = SecurityUtil.getCurrentMemberId(); // JWT에서 회원 ID 추출
        PaymentCreateResponse response = paymentService.createPayment(request, memberId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/success")
    public ResponseEntity<String> paymentSuccess(
            @RequestParam String paymentType,
            @RequestParam String orderId,
            @RequestParam String paymentKey,
            @RequestParam Integer amount) {

        System.out.println("=== Payment Success Called ===");
        System.out.println("PaymentType: " + paymentType);
        System.out.println("OrderId: " + orderId);
        System.out.println("PaymentKey: " + paymentKey);
        System.out.println("Amount: " + amount);

        PaymentConfirmRequest confirmRequest = PaymentConfirmRequest.builder()
                .paymentKey(paymentKey)
                .orderId(orderId)
                .amount(amount)
                .build();

        try {
            System.out.println("Calling paymentService.confirmPayment...");
            PaymentConfirmResponse response = paymentService.confirmPayment(confirmRequest);
            System.out.println("Payment confirmation successful: " + response);
            // 성공 페이지로 리다이렉트
            return ResponseEntity.status(302)
                    .header("Location", "http://localhost:5173/payment/success?orderId=" + orderId)
                    .build();
        } catch (Exception e) {
            System.err.println("Payment confirmation failed: " + e.getMessage());
            e.printStackTrace();
            // 실패 페이지로 리다이렉트 (한글 메시지 제거)
            return ResponseEntity.status(302)
                    .header("Location", "http://localhost:5173/payment/fail?error=payment_failed")
                    .build();
        }
    }

    @PostMapping("/confirm")
    public ResponseEntity<PaymentConfirmResponse> confirmPayment(@RequestBody PaymentConfirmRequest request) {
        PaymentConfirmResponse response = paymentService.confirmPayment(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/member/{memberId}")
    public ResponseEntity<List<PaymentDetailResponse>> getPaymentsByMember(@PathVariable Long memberId) {
        List<PaymentDetailResponse> responses = paymentService.getPaymentsByMember(memberId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/post/{postId}")
    public ResponseEntity<List<PaymentDetailResponse>> getPaymentsByPost(@PathVariable Long postId) {
        List<PaymentDetailResponse> responses = paymentService.getPaymentsByPost(postId);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/my")
    public ResponseEntity<List<PaymentDetailResponse>> getMyPayments() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        List<PaymentDetailResponse> responses = paymentService.getPaymentsByMember(memberId);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/cancel")
    public ResponseEntity<PaymentCancelResponse> cancelPayment(@RequestBody PaymentCancelRequest request) {
        PaymentCancelResponse response = paymentService.cancelPayment(request);
        return ResponseEntity.ok(response);
    }

}
