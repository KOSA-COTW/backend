package cotw.server.domain.payment.controller;

import cotw.server.domain.payment.config.SecurityUtil;
import cotw.server.domain.payment.dto.request.PaymentCancelRequest;
import cotw.server.domain.payment.dto.request.PaymentConfirmRequest;
import cotw.server.domain.payment.dto.request.PaymentCreateRequest;
import cotw.server.domain.payment.dto.response.PaymentCancelResponse;
import cotw.server.domain.payment.dto.response.PaymentCreateResponse;
import cotw.server.domain.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    
    @Value("${app.front-redirect-base}")
    private String frontendBaseUrl;

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
                    .header("Location", frontendBaseUrl + "/payment/success?orderId=" + orderId)
                    .build();
        } catch (Exception e) {
            return ResponseEntity.status(302)
                    .header("Location", frontendBaseUrl + "/payment/fail?error=payment_failed")
                    .build();
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<PaymentCancelResponse> cancelPayment(@RequestBody PaymentCancelRequest request) {
        PaymentCancelResponse response = paymentService.cancelPayment(request);
        return ResponseEntity.ok(response);
    }
}
