package cotw.server.domain.payment;

import cotw.server.domain.board.entity.Category;
import cotw.server.domain.board.entity.Post;
import cotw.server.domain.board.entity.PostVisibility;
import cotw.server.domain.board.repository.PostRepository;
import cotw.server.domain.member.entity.Member;
import cotw.server.domain.member.entity.Role;
import cotw.server.domain.member.repository.MemberRepository;
import cotw.server.domain.payment.entity.PaymentEvent;
import cotw.server.domain.payment.entity.PaymentLedger;
import cotw.server.domain.payment.entity.PaymentOrder;
import cotw.server.domain.payment.entity.PaymentStatus;
import cotw.server.domain.payment.entity.PaymentType;
import cotw.server.domain.payment.repository.PaymentLedgerRepository;
import cotw.server.domain.payment.repository.PaymentOrderRepository;
import cotw.server.domain.payment.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("결제 데이터 무결성 테스트")
class PaymentDataIntegrityTest {

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
                .visibilityStatus(PostVisibility.APPROVED)
                .deadline(LocalDate.of(2025, 10, 1))
                .build();
        testPost = postRepository.save(testPost);
    }

    @Test
    @DisplayName("결제 전체 플로우 - PaymentEvent 생성부터 PaymentLedger까지")
    void paymentFullFlow() {
        // given
        String orderId = "ORDER_DATA_INTEGRITY_001";
        
        // 1. PaymentEvent 생성
        PaymentEvent paymentEvent = PaymentEvent.builder()
                .orderId(orderId)
                .memberId(testMember.getId())
                .postId(testPost.getId())
                .amount(30000)
                .status(PaymentStatus.READY)
                .build();
        paymentEvent = paymentEventRepository.save(paymentEvent);

        // 2. PaymentOrder 생성 (결제 승인 완료)
        PaymentOrder paymentOrder = PaymentOrder.builder()
                .orderId(orderId)
                .paymentKey("payment_key_data_integrity")
                .member(testMember)
                .post(testPost)
                .amount(30000)
                .status(PaymentStatus.DONE)
                .type(PaymentType.NORMAL)
                .rawData("{}")
                .build();
        paymentOrder = paymentOrderRepository.save(paymentOrder);

        // 3. PaymentEvent 상태 업데이트
        paymentEvent.updateStatus(PaymentStatus.DONE);
        paymentEvent = paymentEventRepository.save(paymentEvent);

        // 4. PaymentLedger 생성
        PaymentLedger paymentLedger = PaymentLedger.builder()
                .orderId(orderId)
                .paymentKey("payment_key_data_integrity")
                .memberId(testMember.getId())
                .postId(testPost.getId())
                .amount(30000)
                .status(PaymentStatus.DONE)
                .type(PaymentType.NORMAL)
                .memberName(testMember.getName())
                .postTitle(testPost.getTitle())
                .build();
        paymentLedger = paymentLedgerRepository.save(paymentLedger);

        // then - 데이터 검증
        // PaymentEvent 검증
        Optional<PaymentEvent> savedEvent = paymentEventRepository.findByOrderId(orderId);
        assertThat(savedEvent).isPresent();
        assertThat(savedEvent.get().getStatus()).isEqualTo(PaymentStatus.DONE);
        assertThat(savedEvent.get().getAmount()).isEqualTo(30000);

        // PaymentOrder 검증
        Optional<PaymentOrder> savedOrder = paymentOrderRepository.findByOrderId(orderId);
        assertThat(savedOrder).isPresent();
        assertThat(savedOrder.get().getStatus()).isEqualTo(PaymentStatus.DONE);
        assertThat(savedOrder.get().getMember().getId()).isEqualTo(testMember.getId());
        assertThat(savedOrder.get().getPost().getId()).isEqualTo(testPost.getId());
        assertThat(savedOrder.get().getAmount()).isEqualTo(30000);

        // PaymentLedger 검증
        List<PaymentLedger> ledgers = paymentLedgerRepository.findByMemberIdOrderByCreatedAtDesc(testMember.getId());
        assertThat(ledgers).hasSize(1);
        assertThat(ledgers.get(0).getOrderId()).isEqualTo(orderId);
        assertThat(ledgers.get(0).getAmount()).isEqualTo(30000);
        assertThat(ledgers.get(0).getMemberName()).isEqualTo(testMember.getName());
        assertThat(ledgers.get(0).getPostTitle()).isEqualTo(testPost.getTitle());
    }

    @Test
    @DisplayName("결제 시스템 데이터 일관성 검증")
    void paymentDataConsistency() {
        // given - 여러 결제 데이터 생성
        String orderId1 = "ORDER_CONSISTENCY_001";
        String orderId2 = "ORDER_CONSISTENCY_002";
        
        // 첫 번째 결제
        createFullPaymentData(orderId1, 10000);
        
        // 두 번째 결제
        createFullPaymentData(orderId2, 20000);

        // when & then - 데이터 일관성 검증
        // 회원별 조회
        List<PaymentOrder> memberOrders = paymentOrderRepository.findByMemberIdOrderByCreatedAtDesc(testMember.getId());
        List<PaymentLedger> memberLedgers = paymentLedgerRepository.findByMemberIdOrderByCreatedAtDesc(testMember.getId());
        
        assertThat(memberOrders).hasSize(2);
        assertThat(memberLedgers).hasSize(2);

        // 게시글별 조회
        List<PaymentOrder> postOrders = paymentOrderRepository.findByPostIdOrderByCreatedAtDesc(testPost.getId());
        List<PaymentLedger> postLedgers = paymentLedgerRepository.findByPostIdOrderByCreatedAtDesc(testPost.getId());
        
        assertThat(postOrders).hasSize(2);
        assertThat(postLedgers).hasSize(2);

        // 총 금액 검증
        int totalOrderAmount = memberOrders.stream().mapToInt(PaymentOrder::getAmount).sum();
        int totalLedgerAmount = memberLedgers.stream().mapToInt(PaymentLedger::getAmount).sum();
        
        assertThat(totalOrderAmount).isEqualTo(30000);
        assertThat(totalLedgerAmount).isEqualTo(30000);
    }

    @Test
    @DisplayName("결제 상태별 데이터 검증")
    void paymentStatusValidation() {
        // given
        String readyOrderId = "ORDER_READY_001";
        String doneOrderId = "ORDER_DONE_001";

        // READY 상태 결제
        PaymentEvent readyEvent = PaymentEvent.builder()
                .orderId(readyOrderId)
                .memberId(testMember.getId())
                .postId(testPost.getId())
                .amount(5000)
                .status(PaymentStatus.READY)
                .build();
        paymentEventRepository.save(readyEvent);

        // DONE 상태 결제
        createFullPaymentData(doneOrderId, 15000);

        // when & then
        Optional<PaymentEvent> readyEventSaved = paymentEventRepository.findByOrderId(readyOrderId);
        Optional<PaymentEvent> doneEventSaved = paymentEventRepository.findByOrderId(doneOrderId);

        assertThat(readyEventSaved).isPresent();
        assertThat(readyEventSaved.get().getStatus()).isEqualTo(PaymentStatus.READY);

        assertThat(doneEventSaved).isPresent();
        assertThat(doneEventSaved.get().getStatus()).isEqualTo(PaymentStatus.DONE);

        // DONE 상태만 PaymentOrder와 PaymentLedger가 있어야 함
        Optional<PaymentOrder> readyOrder = paymentOrderRepository.findByOrderId(readyOrderId);
        Optional<PaymentOrder> doneOrder = paymentOrderRepository.findByOrderId(doneOrderId);

        assertThat(readyOrder).isEmpty();
        assertThat(doneOrder).isPresent();
    }

    private void createFullPaymentData(String orderId, int amount) {
        // PaymentEvent 생성
        PaymentEvent event = PaymentEvent.builder()
                .orderId(orderId)
                .memberId(testMember.getId())
                .postId(testPost.getId())
                .amount(amount)
                .status(PaymentStatus.DONE)
                .build();
        paymentEventRepository.save(event);

        // PaymentOrder 생성
        PaymentOrder order = PaymentOrder.builder()
                .orderId(orderId)
                .paymentKey("payment_key_" + orderId)
                .member(testMember)
                .post(testPost)
                .amount(amount)
                .status(PaymentStatus.DONE)
                .type(PaymentType.NORMAL)
                .rawData("{}")
                .build();
        paymentOrderRepository.save(order);

        // PaymentLedger 생성
        PaymentLedger ledger = PaymentLedger.builder()
                .orderId(orderId)
                .paymentKey("payment_key_" + orderId)
                .memberId(testMember.getId())
                .postId(testPost.getId())
                .amount(amount)
                .status(PaymentStatus.DONE)
                .type(PaymentType.NORMAL)
                .memberName(testMember.getName())
                .postTitle(testPost.getTitle())
                .build();
        paymentLedgerRepository.save(ledger);
    }
}