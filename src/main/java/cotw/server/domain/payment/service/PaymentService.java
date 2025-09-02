package cotw.server.domain.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import cotw.server.domain.board.entity.Participant;
import cotw.server.domain.board.entity.Post;
import cotw.server.domain.board.repository.PostRepository;
import cotw.server.domain.donation.service.PaymentEventPublisher;
import cotw.server.domain.member.entity.Member;
import cotw.server.domain.member.repository.MemberRepository;
import cotw.server.domain.payment.config.TossPaymentConfig;
import cotw.server.domain.payment.dto.request.PaymentCancelRequest;
import cotw.server.domain.payment.dto.request.PaymentConfirmRequest;
import cotw.server.domain.payment.dto.request.PaymentCreateRequest;
import cotw.server.domain.payment.dto.response.PaymentCancelResponse;
import cotw.server.domain.payment.dto.response.PaymentCreateResponse;
import cotw.server.domain.payment.dto.response.TossCancelResponse;
import cotw.server.domain.payment.dto.response.TossPaymentResponse;
import cotw.server.domain.payment.entity.*;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
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
    private final TransactionService transactionService;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    private final PaymentEventPublisher events;

    public PaymentCreateResponse createPayment(PaymentCreateRequest request, Long memberId) {
        // orderId 필수 검증
        if (request.getOrderId() == null || request.getOrderId().trim().isEmpty()) {
            throw new PaymentException("orderId는 필수입니다.");
        }

        // 회원 및 게시글 검증
        memberRepository.findById(memberId)
                .orElseThrow(() -> new PaymentException("존재하지 않는 회원입니다."));

        Post post = postRepository.findById(request.getPostId())
                .orElseThrow(() -> new PaymentException("존재하지 않는 게시글입니다."));

        String orderId = request.getOrderId();

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

    public void confirmPayment(PaymentConfirmRequest request) {
        // 0. 트랜잭션 로깅 - 결제 승인 시도
        transactionService.logTransactionAsync(
            request.getOrderId(),
            TransactionType.PAYMENT_CONFIRM,
            "SYSTEM", // 또는 현재 사용자 ID
            request.getPaymentKey(),
            request.getAmount(),
            "/api/payments/confirm"
        );

        // 1. PaymentEvent 조회 및 검증
        PaymentEvent paymentEvent = paymentEventRepository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new PaymentException("존재하지 않는 주문입니다."));

        // 2. 멱등성 검증 - 이미 처리된 결제인지 확인
        if (paymentEvent.isAlreadyProcessed()) {
            PaymentOrder existingOrder = paymentOrderRepository.findByOrderId(request.getOrderId())
                    .orElseThrow(() -> new PaymentException("결제가 완료되었지만 주문 정보를 찾을 수 없습니다."));
            
            postRepository.findById(existingOrder.getPost().getId())
                    .orElseThrow(() -> new PaymentException("존재하지 않는 게시글입니다."));
            
            return;
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
                .paymentMethod(tossResponse.getMethod())
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

        // 9. Participant 추가 (기부자 정보 저장)
        createParticipant(member, post, request.getAmount());

        // 10. 비동기로 PaymentLedger 생성
        ledgerService.createPaymentLedgerAsync(paymentOrder);


        // redis 반영을 위한 코드
        // 11. 카운터 집계 이벤트 발행 (커밋 후 Redis INCR)
        // approvedAt을 Toss 응답에서 구하거나 현재시각으로 대체
        LocalDate paidDate = resolveApprovedAtFromTossOrNow().toLocalDate();
        events.publishCounted(
                null,                            // ledgerId 모르면 null 가능
                post.getId(),                    // 집계 단위: 게시글
                request.getAmount().longValue(), // 금액
                paidDate
        );
    }

    public PaymentCancelResponse cancelPayment(PaymentCancelRequest request) {
        // 1. PaymentOrder 조회 및 검증
        PaymentOrder paymentOrder = paymentOrderRepository.findByPaymentKey(request.getPaymentKey())
                .orElseThrow(() -> new PaymentException("존재하지 않는 결제입니다."));

        // 0. 트랜잭션 로깅 - 결제 취소 시도
        transactionService.logTransactionAsync(
            paymentOrder.getOrderId(),
            TransactionType.PAYMENT_CANCEL,
            "SYSTEM", // 또는 현재 사용자 ID
            request.getPaymentKey(),
            paymentOrder.getAmount(),
            "/api/payments/cancel"
        );

        // 2. 이미 취소된 결제인지 확인 (멱등성 보장)
        if (paymentOrder.getStatus() == PaymentStatus.CANCELED) {
            // 이미 취소된 경우 기존 취소 정보 반환
            return buildCancelResponse(paymentOrder, request.getCancelReason());
        }

        // 3. 취소 가능 상태 검증
        if (paymentOrder.getStatus() != PaymentStatus.DONE) {
            throw new PaymentException("취소할 수 없는 결제 상태입니다. 현재 상태: " + paymentOrder.getStatus());
        }

        // 4. 토스 결제 취소 API 호출
        TossCancelResponse tossResponse;
        try {
            tossResponse = callTossCancelApiAsync(request).get();
        } catch (Exception e) {
            throw new PaymentException("결제 취소 처리 중 오류가 발생했습니다: " + e.getMessage());
        }

        // 5. PaymentOrder 상태 업데이트
        paymentOrder.updateStatus(PaymentStatus.CANCELED);
        paymentOrderRepository.save(paymentOrder);

        // 6. PaymentEvent 상태 업데이트 (낙관적 락으로 동시성 제어)
        PaymentEvent paymentEvent = paymentEventRepository.findByOrderId(paymentOrder.getOrderId())
                .orElseThrow(() -> new PaymentException("결제 이벤트를 찾을 수 없습니다."));
        
        try {
            paymentEvent.updateStatus(PaymentStatus.CANCELED);
            paymentEventRepository.save(paymentEvent);
        } catch (Exception e) {
            // 낙관적 락 실패 시 이미 다른 요청에서 처리된 것으로 간주
            throw new PaymentIdempotencyException("동시 처리로 인해 결제 취소가 중복 처리되었습니다.");
        }

        // 7. 비즈니스 로직 처리 (Post 금액 차감, Participant 삭제)
        handleCancellationBusinessLogic(paymentOrder, request.getCancelAmount());

        // 8. 비동기로 PaymentLedger 취소 상태로 업데이트
        ledgerService.updateLedgerToCanceledAsync(paymentOrder, request.getCancelReason());

        // 이전 상태가 DONE이었다면 차감 이벤트 발행 (커밋 후 Redis DECR)
        // 기준 날짜: 원 결제일(주문 생성일이 있으면 사용), 없으면 now
        LocalDate baseDate = (paymentOrder.getCreatedAt() != null)
                ? paymentOrder.getCreatedAt().toLocalDate()
                : LocalDate.now();


        // redis 반영을 위한 코드
        // 취소 금액 계산 (부분취소 대응)
        Integer actualCancelAmount = (request.getCancelAmount() != null)
                ? request.getCancelAmount()
                : paymentOrder.getAmount();

        if (/* 이전이 DONE이었는지 확인 필요 시 */ true) {
            events.publishReversed(
                    null,
                    paymentOrder.getPost().getId(),
                    actualCancelAmount.longValue(),
                    baseDate
            );
        }

        return buildCancelResponse(paymentOrder, request.getCancelReason());
    }

    @CircuitBreaker(name = "paymentService", fallbackMethod = "fallbackTossCancelApi")
    @Retry(name = "paymentService")
    @Bulkhead(name = "paymentService")
    @TimeLimiter(name = "paymentService")
    public CompletableFuture<TossCancelResponse> callTossCancelApiAsync(PaymentCancelRequest request) {
        return CompletableFuture.supplyAsync(() -> callTossCancelApi(request));
    }

    private TossCancelResponse callTossCancelApi(PaymentCancelRequest request) {
        String encodedSecretKey = Base64.getEncoder()
                .encodeToString((tossConfig.getSecretKey() + ":").getBytes());

        try {
            // 취소 요청 body 구성
            String requestBody = createCancelRequestBody(request);
            
            return restClient.post()
                    .uri(tossConfig.getApiUrl() + "/v1/payments/" + request.getPaymentKey() + "/cancel")
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + encodedSecretKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(requestBody)
                    .retrieve()
                    .body(TossCancelResponse.class);
            
        } catch (Exception e) {
            throw new PaymentException("토스 결제 취소 API 호출 실패: " + e.getMessage());
        }
    }

    public TossCancelResponse fallbackTossCancelApi(PaymentCancelRequest request, Exception ex) {
        throw new PaymentException("결제 취소 서비스가 일시적으로 불가능합니다. 잠시 후 다시 시도해주세요.");
    }

    private String createCancelRequestBody(PaymentCancelRequest request) {
        try {
            // 취소 요청을 위한 Map 생성
            var cancelRequest = new java.util.HashMap<String, Object>();
            cancelRequest.put("cancelReason", request.getCancelReason());
            
            // 부분 취소인 경우 취소 금액 포함
            if (request.getCancelAmount() != null) {
                cancelRequest.put("cancelAmount", request.getCancelAmount());
            }
            
            return objectMapper.writeValueAsString(cancelRequest);
        } catch (Exception e) {
            throw new PaymentException("취소 요청 데이터 생성 실패: " + e.getMessage());
        }
    }

    private void handleCancellationBusinessLogic(PaymentOrder paymentOrder, Integer cancelAmount) {
        Post post = paymentOrder.getPost();
        Member member = paymentOrder.getMember();
        
        // 전액 취소인지 부분 취소인지 확인
        Integer actualCancelAmount = (cancelAmount != null) ? cancelAmount : paymentOrder.getAmount();
        
        // Post의 currentAmount에서 취소 금액 차감
        post.subtractDonationAmount(actualCancelAmount);
        postRepository.save(post);
        
        // 전액 취소인 경우에만 Participant 삭제
        if (cancelAmount == null || cancelAmount.equals(paymentOrder.getAmount())) {
            removeParticipant(member, post);
        }
    }

    private void removeParticipant(Member member, Post post) {
        // Participant 삭제 로직
        post.getParticipants().removeIf(participant -> 
            participant.getMember().getId().equals(member.getId()));
    }

    private PaymentCancelResponse buildCancelResponse(PaymentOrder paymentOrder, String cancelReason) {
        Post post = paymentOrder.getPost();
        
        return PaymentCancelResponse.builder()
                .orderId(paymentOrder.getOrderId())
                .paymentKey(paymentOrder.getPaymentKey())
                .status(PaymentStatus.CANCELED)
                .type(paymentOrder.getType())
                .totalAmount(paymentOrder.getAmount())
                .cancelAmount(paymentOrder.getAmount()) // 현재는 전액 취소만 지원
                .balanceAmount(0) // 전액 취소이므로 잔여 금액은 0
                .cancelReason(cancelReason)
                .canceledAt(java.time.OffsetDateTime.now())
                .orderName(post.getTitle())
                .build();
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

    private LocalDateTime resolveApprovedAtFromTossOrNow() {
        // TossPaymentResponse에 approvedAt이 있으면 그걸 쓰고,
        // DTO에 없으면 일단 now() 사용 (필요하면 Toss 응답 필드 연결)
        return LocalDateTime.now();
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
    }
}