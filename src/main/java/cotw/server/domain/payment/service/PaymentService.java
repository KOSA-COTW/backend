package cotw.server.domain.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cotw.server.domain.board.entity.Post;
import cotw.server.domain.board.repository.PostRepository;
import cotw.server.domain.member.entity.Member;
import cotw.server.domain.member.repository.MemberRepository;
import cotw.server.domain.payment.config.OrderIdGenerator;
import cotw.server.domain.payment.config.TossPaymentConfig;
import cotw.server.domain.payment.dto.request.PaymentConfirmRequest;
import cotw.server.domain.payment.dto.request.PaymentCreateRequest;
import cotw.server.domain.payment.dto.response.PaymentConfirmResponse;
import cotw.server.domain.payment.dto.response.PaymentCreateResponse;
import cotw.server.domain.payment.dto.response.PaymentDetailResponse;
import cotw.server.domain.payment.dto.response.TossPaymentResponse;
import cotw.server.domain.payment.entity.PaymentEvent;
import cotw.server.domain.payment.entity.PaymentOrder;
import cotw.server.domain.payment.entity.PaymentStatus;
import cotw.server.domain.payment.entity.PaymentType;
import cotw.server.domain.payment.exception.PaymentException;
import cotw.server.domain.payment.repository.PaymentOrderRepository;
import cotw.server.domain.payment.repository.PaymentRepository;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final PaymentRepository paymentEventRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final MemberRepository memberRepository;
    private final PostRepository postRepository;
    private final TossPaymentConfig tossConfig;
    private final LedgerService ledgerService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final OrderIdGenerator orderIdGenerator;

    public PaymentCreateResponse createPayment(PaymentCreateRequest request, Long memberId) {
        // 회원 및 게시글 검증
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new PaymentException("존재하지 않는 회원입니다."));

        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new PaymentException("존재하지 않는 게시글입니다."));

        // orderId 생성 (OrderIdGenerator 사용)
        String orderId = orderIdGenerator.generateOrderId();

        // PaymentEvent 생성 및 저장
        PaymentEvent paymentEvent = PaymentEvent.builder()
                .orderId(orderId)
                .memberId(memberId)
                .postId(request.getPostId())
                .amount(request.getAmount())
                .status(PaymentStatus.READY)
                .type(PaymentType.NORMAL)
                .build();

        paymentEventRepository.save(paymentEvent);

        return PaymentCreateResponse.builder()
                .orderId(orderId)
                .amount(request.getAmount())
                .orderName(post.getTitle())
                .build();
    }

    public PaymentConfirmResponse confirmPayment(PaymentConfirmRequest request) {
        // 1. PaymentEvent 조회 및 검증
        PaymentEvent paymentEvent = paymentEventRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new PaymentException("존재하지 않는 주문입니다."));

        // 2. 금액 검증
        if (!paymentEvent.getAmount().equals(request.getAmount())) {
            throw new PaymentException("결제 금액이 일치하지 않습니다.");
        }

        // 3. 토스 결제 승인 API 호출 (비동기)
        TossPaymentResponse tossResponse;
        try {
            tossResponse = callTossConfirmApiAsync(request).get();
        } catch (Exception e) {
            throw new PaymentException("결제 승인 처리 중 오류가 발생했습니다: " + e.getMessage());
        }

        // 4. PaymentOrder 생성
        Member member = memberRepository.findById(paymentEvent.getMemberId())
                .orElseThrow(() -> new PaymentException("존재하지 않는 회원입니다."));

        Post post = postRepository.findById(paymentEvent.getPostId())
                .orElseThrow(() -> new PaymentException("존재하지 않는 게시글입니다."));

        PaymentOrder paymentOrder = PaymentOrder.builder()
                .orderId(request.getOrderId())
                .paymentKey(request.getPaymentKey())
                .member(member)
                .post(post)
                .amount(request.getAmount())
                .status(PaymentStatus.DONE)
                .type(PaymentType.NORMAL)
                .rawData(convertToJson(tossResponse))
                .build();

        paymentOrderRepository.save(paymentOrder);

        // 5. PaymentEvent 상태 업데이트
        paymentEvent.updateStatus(PaymentStatus.DONE);

        // 6. 비동기로 PaymentLedger 생성
        ledgerService.createPaymentLedgerAsync(paymentOrder);

        return PaymentConfirmResponse.builder()
                .orderId(paymentOrder.getOrderId())
                .paymentKey(paymentOrder.getPaymentKey())
                .status(paymentOrder.getStatus())
                .type(paymentOrder.getType())
                .amount(paymentOrder.getAmount())
                .orderName(post.getTitle())
                .build();
    }

    @Transactional(readOnly = true)
    public List<PaymentDetailResponse> getPaymentsByMember(Long memberId) {
        List<PaymentOrder> orders = paymentOrderRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
        return orders.stream()
                .map(this::convertToDetailResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentDetailResponse> getPaymentsByPost(Long postId) {
        List<PaymentOrder> orders = paymentOrderRepository.findByPostIdOrderByCreatedAtDesc(postId);
        return orders.stream()
                .map(this::convertToDetailResponse)
                .toList();
    }

    @CircuitBreaker(name = "paymentService", fallbackMethod = "fallbackTossConfirmApi")
    @Retry(name = "paymentService")
    @Bulkhead(name = "paymentService")
    @TimeLimiter(name = "paymentService")
    public CompletableFuture<TossPaymentResponse> callTossConfirmApiAsync(PaymentConfirmRequest request) {
        return CompletableFuture.supplyAsync(() -> callTossConfirmApi(request));
    }

    private TossPaymentResponse callTossConfirmApi(PaymentConfirmRequest request) {
        String encodedSecretKey = Base64.getEncoder()
                .encodeToString((tossConfig.getSecretKey() + ":").getBytes());

        try {
            return restClient.post()
                    .uri(tossConfig.getApiUrl() + "/v1/payments/confirm")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedSecretKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(request)
                    .retrieve()
                    .body(TossPaymentResponse.class);
        } catch (Exception e) {
            throw new PaymentException("토스 결제 승인 API 호출 실패: " + e.getMessage());
        }
    }

    public TossPaymentResponse fallbackTossConfirmApi(PaymentConfirmRequest request, Exception ex) {
        throw new PaymentException("결제 서비스가 일시적으로 불가능합니다. 잠시 후 다시 시도해주세요.");
    }

    private String convertToJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    private PaymentDetailResponse convertToDetailResponse(PaymentOrder order) {
        return PaymentDetailResponse.builder()
                .id(order.getId())
                .orderId(order.getOrderId())
                .paymentKey(order.getPaymentKey())
                .memberName(order.getMember().getName())
                .postTitle(order.getPost().getTitle())
                .amount(order.getAmount())
                .status(order.getStatus())
                .type(order.getType())
                .createdAt(order.getCreatedAt())
                .build();
    }
}