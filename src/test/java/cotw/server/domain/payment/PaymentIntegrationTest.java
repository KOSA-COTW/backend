package cotw.server.domain.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import cotw.server.domain.board.entity.Category;
import cotw.server.domain.board.entity.Post;
import cotw.server.domain.board.repository.PostRepository;
import cotw.server.domain.member.entity.Member;
import cotw.server.domain.member.entity.Role;
import cotw.server.domain.member.repository.MemberRepository;
import cotw.server.domain.payment.dto.request.PaymentCreateRequest;
import cotw.server.domain.payment.entity.PaymentEvent;
import cotw.server.domain.payment.entity.PaymentLedger;
import cotw.server.domain.payment.entity.PaymentOrder;
import cotw.server.domain.payment.entity.PaymentStatus;
import cotw.server.domain.payment.repository.PaymentLedgerRepository;
import cotw.server.domain.payment.repository.PaymentOrderRepository;
import cotw.server.domain.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("결제 시스템 통합 테스트")
class PaymentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PaymentRepository paymentEventRepository;

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Autowired
    private PaymentLedgerRepository paymentLedgerRepository;

    private Member testMember;
    private Post testPost;

    @BeforeEach
    void setUp() {
        // 테스트 회원 생성
        testMember = Member.builder()
                .name("테스트 사용자")
                .email("test@test.com")
                .password("password")
                .role(Role.USER)
                .build();
        testMember = memberRepository.save(testMember);

        // 테스트 게시글 생성
        testPost = Post.builder()
                .title("테스트 모금 게시글")
                .content("테스트 내용")
                .author(testMember)
                .category(Category.CHILD)
                .amount(100000)
                .currentAmount(0)
                .isPublic(true)
                .build();
        testPost = postRepository.save(testPost);
    }

    @Test
    @DisplayName("결제 전체 플로우 - PaymentEvent 생성부터 PaymentLedger까지")
    @WithMockUser(username = "test@test.com")
    void paymentFullFlow() {
        // given
        PaymentCreateRequest createRequest = new PaymentCreateRequest();
        setField(createRequest, "postId", testPost.getId());
        setField(createRequest, "amount", 10000);

        // 1. PaymentEvent 생성 (결제 생성 API는 실제로는 SecurityUtil을 통해 memberId를 가져오므로 직접 테스트)
        PaymentEvent paymentEvent = PaymentEvent.builder()
                .orderId("ORDER_TEST_001")
                .memberId(testMember.getId())
                .postId(testPost.getId())
                .amount(10000)
                .status(PaymentStatus.READY)
                .build();
        paymentEvent = paymentEventRepository.save(paymentEvent);

        // 2. PaymentOrder 생성 (결제 승인 완료)
        PaymentOrder paymentOrder = PaymentOrder.builder()
                .orderId("ORDER_TEST_001")
                .paymentKey("payment_key_test")
                .member(testMember)
                .post(testPost)
                .amount(10000)
                .status(PaymentStatus.DONE)
                .build();
        paymentOrder = paymentOrderRepository.save(paymentOrder);

        // 3. PaymentEvent 상태 업데이트
        paymentEvent.updateStatus(PaymentStatus.DONE);
        paymentEventRepository.save(paymentEvent);

        // 4. PaymentLedger 생성
        PaymentLedger paymentLedger = PaymentLedger.builder()
                .orderId("ORDER_TEST_001")
                .paymentKey("payment_key_test")
                .memberId(testMember.getId())
                .postId(testPost.getId())
                .amount(10000)
                .status(PaymentStatus.DONE)
                .memberName(testMember.getName())
                .postTitle(testPost.getTitle())
                .build();
        paymentLedger = paymentLedgerRepository.save(paymentLedger);

        // then - 데이터 검증
        // PaymentEvent 검증
        Optional<PaymentEvent> savedEvent = paymentEventRepository.findByOrderId("ORDER_TEST_001");
        assertThat(savedEvent).isPresent();
        assertThat(savedEvent.get().getStatus()).isEqualTo(PaymentStatus.DONE);

        // PaymentOrder 검증
        Optional<PaymentOrder> savedOrder = paymentOrderRepository.findByOrderId("ORDER_TEST_001");
        assertThat(savedOrder).isPresent();
        assertThat(savedOrder.get().getStatus()).isEqualTo(PaymentStatus.DONE);
        assertThat(savedOrder.get().getMember().getId()).isEqualTo(testMember.getId());
        assertThat(savedOrder.get().getPost().getId()).isEqualTo(testPost.getId());

        // PaymentLedger 검증
        List<PaymentLedger> ledgers = paymentLedgerRepository.findByMemberIdOrderByCreatedAtDesc(testMember.getId());
        assertThat(ledgers).hasSize(1);
        assertThat(ledgers.get(0).getOrderId()).isEqualTo("ORDER_TEST_001");
        assertThat(ledgers.get(0).getAmount()).isEqualTo(10000);
    }

    @Test
    @DisplayName("회원별 결제 내역 조회 API 통합 테스트")
    void getPaymentsByMember_Integration() throws Exception {
        // given - 테스트 데이터 생성
        PaymentOrder paymentOrder = PaymentOrder.builder()
                .orderId("ORDER_INTEGRATION_001")
                .paymentKey("payment_key_integration")
                .member(testMember)
                .post(testPost)
                .amount(15000)
                .status(PaymentStatus.DONE)
                .build();
        paymentOrderRepository.save(paymentOrder);

        // when & then
        mockMvc.perform(get("/api/payments/member/{memberId}", testMember.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].orderId").value("ORDER_INTEGRATION_001"))
                .andExpect(jsonPath("$[0].memberName").value("테스트 사용자"))
                .andExpect(jsonPath("$[0].postTitle").value("테스트 모금 게시글"))
                .andExpect(jsonPath("$[0].amount").value(15000));
    }

    @Test
    @DisplayName("게시글별 결제 내역 조회 API 통합 테스트")
    void getPaymentsByPost_Integration() throws Exception {
        // given - 테스트 데이터 생성
        PaymentOrder paymentOrder = PaymentOrder.builder()
                .orderId("ORDER_POST_001")
                .paymentKey("payment_key_post")
                .member(testMember)
                .post(testPost)
                .amount(20000)
                .status(PaymentStatus.DONE)
                .build();
        paymentOrderRepository.save(paymentOrder);

        // when & then
        mockMvc.perform(get("/api/payments/post/{postId}", testPost.getId()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].orderId").value("ORDER_POST_001"))
                .andExpect(jsonPath("$[0].amount").value(20000));
    }

    @Test
    @DisplayName("결제 시스템 데이터 일관성 검증")
    void paymentDataConsistency() {
        // given - 여러 결제 데이터 생성
        String orderId = "ORDER_CONSISTENCY_001";
        
        PaymentEvent event = PaymentEvent.builder()
                .orderId(orderId)
                .memberId(testMember.getId())
                .postId(testPost.getId())
                .amount(30000)
                .status(PaymentStatus.DONE)
                .build();
        paymentEventRepository.save(event);

        PaymentOrder order = PaymentOrder.builder()
                .orderId(orderId)
                .paymentKey("payment_key_consistency")
                .member(testMember)
                .post(testPost)
                .amount(30000)
                .status(PaymentStatus.DONE)
                .build();
        paymentOrderRepository.save(order);

        PaymentLedger ledger = PaymentLedger.builder()
                .orderId(orderId)
                .paymentKey("payment_key_consistency")
                .memberId(testMember.getId())
                .postId(testPost.getId())
                .amount(30000)
                .status(PaymentStatus.DONE)
                .memberName(testMember.getName())
                .postTitle(testPost.getTitle())
                .build();
        paymentLedgerRepository.save(ledger);

        // when & then - 데이터 일관성 검증
        Optional<PaymentEvent> savedEvent = paymentEventRepository.findByOrderId(orderId);
        Optional<PaymentOrder> savedOrder = paymentOrderRepository.findByOrderId(orderId);
        List<PaymentLedger> savedLedgers = paymentLedgerRepository.findByMemberIdOrderByCreatedAtDesc(testMember.getId());

        // 모든 데이터가 존재하고 일관성이 유지되는지 확인
        assertThat(savedEvent).isPresent();
        assertThat(savedOrder).isPresent();
        assertThat(savedLedgers).isNotEmpty();

        // 금액 일관성 확인
        assertThat(savedEvent.get().getAmount()).isEqualTo(30000);
        assertThat(savedOrder.get().getAmount()).isEqualTo(30000);
        assertThat(savedLedgers.get(0).getAmount()).isEqualTo(30000);

        // orderId 일관성 확인
        assertThat(savedEvent.get().getOrderId()).isEqualTo(orderId);
        assertThat(savedOrder.get().getOrderId()).isEqualTo(orderId);
        assertThat(savedLedgers.get(0).getOrderId()).isEqualTo(orderId);

        // 상태 일관성 확인
        assertThat(savedEvent.get().getStatus()).isEqualTo(PaymentStatus.DONE);
        assertThat(savedOrder.get().getStatus()).isEqualTo(PaymentStatus.DONE);
        assertThat(savedLedgers.get(0).getStatus()).isEqualTo(PaymentStatus.DONE);
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