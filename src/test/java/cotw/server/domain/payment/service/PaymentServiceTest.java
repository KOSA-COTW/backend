package cotw.server.domain.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import cotw.server.domain.board.entity.Category;
import cotw.server.domain.board.entity.Post;
import cotw.server.domain.board.repository.PostRepository;
import cotw.server.domain.member.entity.Member;
import cotw.server.domain.member.entity.Role;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService 테스트")
class PaymentServiceTest {

    @InjectMocks
    private PaymentService paymentService;

    @Mock
    private PaymentRepository paymentEventRepository;

    @Mock
    private PaymentOrderRepository paymentOrderRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private TossPaymentConfig tossConfig;

    @Mock
    private LedgerService ledgerService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private RestClient restClient;

    @Mock
    private OrderIdGenerator orderIdGenerator;

    private Member testMember;
    private Post testPost;
    private PaymentEvent testPaymentEvent;
    private PaymentOrder testPaymentOrder;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .id(1L)
                .name("테스트 사용자")
                .email("test@test.com")
                .password("password")
                .role(Role.USER)
                .createdDate(LocalDateTime.now())
                .build();

        testPost = Post.builder()
                .id(1L)
                .title("테스트 모금 게시글")
                .content("테스트 내용")
                .author(testMember)
                .category(Category.CHILD)
                .amount(100000)
                .currentAmount(0)
                .isPublic(true)
                .build();

        testPaymentEvent = PaymentEvent.builder()
                .id(1L)
                .orderId("ORDER_20240815_001")
                .memberId(1L)
                .postId(1L)
                .amount(10000)
                .status(PaymentStatus.READY)
                .type(PaymentType.NORMAL)
                .build();

        testPaymentOrder = PaymentOrder.builder()
                .id(1L)
                .orderId("ORDER_20240815_001")
                .paymentKey("payment_key_123")
                .member(testMember)
                .post(testPost)
                .amount(10000)
                .status(PaymentStatus.DONE)
                .type(PaymentType.NORMAL)
                .rawData("{}")
                .build();
    }

    @Test
    @DisplayName("결제 생성 - 성공")
    void createPayment_Success() {
        // given
        PaymentCreateRequest request = new PaymentCreateRequest();
        // Reflection을 사용하여 private 필드 설정
        setField(request, "postId", 1L);
        setField(request, "amount", 10000);
        
        Long memberId = 1L;
        String generatedOrderId = "ORDER_20240815_001";

        given(memberRepository.findById(memberId)).willReturn(Optional.of(testMember));
        given(postRepository.findById(1L)).willReturn(Optional.of(testPost));
        given(orderIdGenerator.generateOrderId()).willReturn(generatedOrderId);
        given(paymentEventRepository.save(any(PaymentEvent.class))).willReturn(testPaymentEvent);

        // when
        PaymentCreateResponse response = paymentService.createPayment(request, memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo(generatedOrderId);
        assertThat(response.getAmount()).isEqualTo(10000);
        assertThat(response.getOrderName()).isEqualTo("테스트 모금 게시글");

        verify(paymentEventRepository).save(any(PaymentEvent.class));
    }

    @Test
    @DisplayName("결제 생성 - 존재하지 않는 회원")
    void createPayment_MemberNotFound() {
        // given
        PaymentCreateRequest request = new PaymentCreateRequest();
        setField(request, "postId", 1L);
        setField(request, "amount", 10000);
        
        Long memberId = 999L;

        given(memberRepository.findById(memberId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentService.createPayment(request, memberId))
                .isInstanceOf(PaymentException.class)
                .hasMessage("존재하지 않는 회원입니다.");
    }

    @Test
    @DisplayName("결제 생성 - 존재하지 않는 게시글")
    void createPayment_PostNotFound() {
        // given
        PaymentCreateRequest request = new PaymentCreateRequest();
        setField(request, "postId", 999L);
        setField(request, "amount", 10000);
        
        Long memberId = 1L;

        given(memberRepository.findById(memberId)).willReturn(Optional.of(testMember));
        given(postRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentService.createPayment(request, memberId))
                .isInstanceOf(PaymentException.class)
                .hasMessage("존재하지 않는 게시글입니다.");
    }

    @Test
    @DisplayName("결제 승인 - 성공")
    void confirmPayment_Success() throws Exception {
        // given
        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
                .paymentKey("payment_key_123")
                .orderId("ORDER_20240815_001")
                .amount(10000)
                .build();

        TossPaymentResponse tossResponse = new TossPaymentResponse();
        setField(tossResponse, "paymentKey", "payment_key_123");
        setField(tossResponse, "orderId", "ORDER_20240815_001");
        setField(tossResponse, "status", "DONE");
        setField(tossResponse, "totalAmount", 10000);
        setField(tossResponse, "method", "카드");

        given(paymentEventRepository.findByOrderId("ORDER_20240815_001"))
                .willReturn(Optional.of(testPaymentEvent));
        given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
        given(postRepository.findById(1L)).willReturn(Optional.of(testPost));
        given(objectMapper.writeValueAsString(any())).willReturn("{}");
        given(paymentOrderRepository.save(any(PaymentOrder.class))).willReturn(testPaymentOrder);

        // PaymentService의 callTossConfirmApiAsync 메서드를 스파이로 설정
        PaymentService spyPaymentService = spy(paymentService);
        doReturn(CompletableFuture.completedFuture(tossResponse))
                .when(spyPaymentService).callTossConfirmApiAsync(any());

        // when
        PaymentConfirmResponse response = spyPaymentService.confirmPayment(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getOrderId()).isEqualTo("ORDER_20240815_001");
        assertThat(response.getPaymentKey()).isEqualTo("payment_key_123");
        assertThat(response.getAmount()).isEqualTo(10000);
        assertThat(response.getStatus()).isEqualTo(PaymentStatus.DONE);

        verify(paymentOrderRepository).save(any(PaymentOrder.class));
        verify(ledgerService).createPaymentLedgerAsync(any(PaymentOrder.class));
    }

    @Test
    @DisplayName("결제 승인 - 존재하지 않는 주문")
    void confirmPayment_OrderNotFound() {
        // given
        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
                .paymentKey("payment_key_123")
                .orderId("INVALID_ORDER")
                .amount(10000)
                .build();

        given(paymentEventRepository.findByOrderId("INVALID_ORDER"))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> paymentService.confirmPayment(request))
                .isInstanceOf(PaymentException.class)
                .hasMessage("존재하지 않는 주문입니다.");
    }

    @Test
    @DisplayName("결제 승인 - 금액 불일치")
    void confirmPayment_AmountMismatch() {
        // given
        PaymentConfirmRequest request = PaymentConfirmRequest.builder()
                .paymentKey("payment_key_123")
                .orderId("ORDER_20240815_001")
                .amount(20000) // 다른 금액
                .build();

        given(paymentEventRepository.findByOrderId("ORDER_20240815_001"))
                .willReturn(Optional.of(testPaymentEvent));

        // when & then
        assertThatThrownBy(() -> paymentService.confirmPayment(request))
                .isInstanceOf(PaymentException.class)
                .hasMessage("결제 금액이 일치하지 않습니다.");
    }

    @Test
    @DisplayName("회원별 결제 내역 조회")
    void getPaymentsByMember_Success() {
        // given
        Long memberId = 1L;
        List<PaymentOrder> orders = List.of(testPaymentOrder);

        given(paymentOrderRepository.findByMemberIdOrderByCreatedAtDesc(memberId))
                .willReturn(orders);

        // when
        List<PaymentDetailResponse> responses = paymentService.getPaymentsByMember(memberId);

        // then
        assertThat(responses).hasSize(1);
        PaymentDetailResponse response = responses.get(0);
        assertThat(response.getOrderId()).isEqualTo("ORDER_20240815_001");
        assertThat(response.getMemberName()).isEqualTo("테스트 사용자");
        assertThat(response.getPostTitle()).isEqualTo("테스트 모금 게시글");
        assertThat(response.getAmount()).isEqualTo(10000);
    }

    @Test
    @DisplayName("게시글별 결제 내역 조회")
    void getPaymentsByPost_Success() {
        // given
        Long postId = 1L;
        List<PaymentOrder> orders = List.of(testPaymentOrder);

        given(paymentOrderRepository.findByPostIdOrderByCreatedAtDesc(postId))
                .willReturn(orders);

        // when
        List<PaymentDetailResponse> responses = paymentService.getPaymentsByPost(postId);

        // then
        assertThat(responses).hasSize(1);
        PaymentDetailResponse response = responses.get(0);
        assertThat(response.getOrderId()).isEqualTo("ORDER_20240815_001");
        assertThat(response.getAmount()).isEqualTo(10000);
    }

    // private 필드 설정을 위한 헬퍼 메서드
    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field: " + fieldName, e);
        }
    }
}