package cotw.server.domain.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cotw.server.domain.board.entity.Participant;
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
import cotw.server.domain.payment.exception.PaymentIdempotencyException;
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

        // orderId 사용 (프론트엔드에서 전달받은 것을 우선 사용, 없으면 생성)
        String orderId = request.getOrderId() != null ? request.getOrderId() : orderIdGenerator.generateOrderId();
        
        System.out.println("Creating PaymentEvent with orderId: " + orderId);

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
        System.out.println("Looking for PaymentEvent with orderId: " + request.getOrderId());
        
        // DB에 있는 모든 PaymentEvent 조회해서 로깅
        List<PaymentEvent> allEvents = paymentEventRepository.findAll();
        System.out.println("All PaymentEvents in DB:");
        for (PaymentEvent event : allEvents) {
            System.out.println("  - OrderId: " + event.getOrderId() + ", Status: " + event.getStatus());
        }
        
        PaymentEvent paymentEvent = paymentEventRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new PaymentException("존재하지 않는 주문입니다."));

        // 2. 멱등성 검증 - 이미 처리된 결제인지 확인
        if (paymentEvent.isAlreadyProcessed()) {
            System.out.println("Payment already processed for orderId: " + request.getOrderId());
            // 이미 처리된 경우 기존 PaymentOrder 반환
            PaymentOrder existingOrder = paymentOrderRepository.findByOrderId(request.getOrderId())
                    .orElseThrow(() -> new PaymentException("결제가 완료되었지만 주문 정보를 찾을 수 없습니다."));
            
            Post post = postRepository.findById(existingOrder.getPost().getId())
                    .orElseThrow(() -> new PaymentException("존재하지 않는 게시글입니다."));
            
            return PaymentConfirmResponse.builder()
                    .orderId(existingOrder.getOrderId())
                    .paymentKey(existingOrder.getPaymentKey())
                    .status(existingOrder.getStatus())
                    .type(existingOrder.getType())
                    .amount(existingOrder.getAmount())
                    .orderName(post.getTitle())
                    .build();
        }

        // 3. 금액 검증
        if (!paymentEvent.getAmount().equals(request.getAmount())) {
            throw new PaymentException("결제 금액이 일치하지 않습니다.");
        }

        // 4. 중복 PaymentOrder 생성 방지 검증
        if (paymentOrderRepository.existsByOrderId(request.getOrderId())) {
            throw new PaymentIdempotencyException("이미 처리된 주문입니다.");
        }

        // 5. 토스 결제 승인 API 호출 (비동기)
        TossPaymentResponse tossResponse;
        try {
            tossResponse = callTossConfirmApiAsync(request).get();
        } catch (Exception e) {
            throw new PaymentException("결제 승인 처리 중 오류가 발생했습니다: " + e.getMessage());
        }

        // 6. PaymentOrder 생성
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

        // 7. PaymentEvent 상태 업데이트 (낙관적 락으로 동시성 제어)
        try {
            paymentEvent.updateStatus(PaymentStatus.DONE);
            paymentEventRepository.save(paymentEvent);
        } catch (Exception e) {
            // 낙관적 락 실패 시 이미 다른 요청에서 처리된 것으로 간주
            throw new PaymentIdempotencyException("동시 처리로 인해 결제가 중복 처리되었습니다.");
        }

        // 8. Post의 currentAmount 업데이트 (기부 금액 추가)
        post.addDonationAmount(request.getAmount());
        postRepository.save(post);
        System.out.println("Post currentAmount updated: " + post.getCurrentAmount());

        // 9. Participant 추가 (기부자 정보 저장)
        createParticipant(member, post, request.getAmount());

        // 10. 비동기로 PaymentLedger 생성
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
            // 토스 API 호출 전 최종 상태 확인 (네트워크 지연으로 인한 중복 호출 방지)
            PaymentEvent finalCheck = paymentEventRepository.findByOrderId(request.getOrderId())
                    .orElseThrow(() -> new PaymentException("결제 정보를 찾을 수 없습니다."));
            
            if (finalCheck.isAlreadyProcessed()) {
                throw new PaymentIdempotencyException("이미 처리된 결제입니다. 중복 처리를 방지합니다.");
            }

            System.out.println("Calling Toss API for orderId: " + request.getOrderId());
            return restClient.post()
                    .uri(tossConfig.getApiUrl() + "/v1/payments/confirm")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedSecretKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(request)
                    .retrieve()
                    .body(TossPaymentResponse.class);
        } catch (Exception e) {
            System.err.println("Toss API call failed for orderId: " + request.getOrderId() + ", Error: " + e.getMessage());
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
    
    private void createParticipant(Member member, Post post, Integer amount) {
        // 기부 참여자 정보 생성
        Participant participant = Participant.builder()
                .member(member)
                .post(post)
                .amount(amount)
                .build();
        
        // Post에 참여자 추가 (cascade로 자동 저장됨)
        post.addParticipant(participant);
        
        System.out.println("Participant added: " + member.getName() + " donated " + amount + " to " + post.getTitle());
    }
}