package cotw.server.domain.payment.service;

import cotw.server.domain.board.entity.Category;
import cotw.server.domain.board.entity.Post;
import cotw.server.domain.board.entity.PostVisibility;
import cotw.server.domain.member.entity.Member;
import cotw.server.domain.member.entity.Role;
import cotw.server.domain.payment.entity.PaymentLedger;
import cotw.server.domain.payment.entity.PaymentOrder;
import cotw.server.domain.payment.entity.PaymentStatus;
import cotw.server.domain.payment.entity.PaymentType;
import cotw.server.domain.payment.repository.PaymentLedgerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("LedgerService 테스트")
class LedgerServiceTest {

    @InjectMocks
    private LedgerService ledgerService;

    @Mock
    private PaymentLedgerRepository paymentLedgerRepository;

    private Member testMember;
    private Post testPost;
    private PaymentOrder testPaymentOrder;
    private PaymentLedger testPaymentLedger;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .id(1L)
                .name("테스트 사용자")
                .email("test@test.com")
                .password("password")
                .role(Role.USER)
                .build();

        testPost = Post.builder()
                .id(1L)
                .title("테스트 모금 게시글")
                .content("테스트 내용")
                .author(testMember)
                .category(Category.CHILD)
                .amount(100000)
                .currentAmount(0)
                .visibilityStatus(PostVisibility.APPROVED)
                .deadline(LocalDate.of(2025, 10, 1))
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

        testPaymentLedger = PaymentLedger.builder()
                .id(1L)
                .orderId("ORDER_20240815_001")
                .paymentKey("payment_key_123")
                .memberId(1L)
                .postId(1L)
                .amount(10000)
                .status(PaymentStatus.DONE)
                .type(PaymentType.NORMAL)
                .memberName("테스트 사용자")
                .postTitle("테스트 모금 게시글")
                .build();
    }

    @Test
    @DisplayName("PaymentLedger 비동기 생성 - 성공")
    void createPaymentLedgerAsync_Success() {
        // given
        given(paymentLedgerRepository.save(any(PaymentLedger.class)))
                .willReturn(testPaymentLedger);

        // when
        ledgerService.createPaymentLedgerAsync(testPaymentOrder);

        // then
        verify(paymentLedgerRepository).save(any(PaymentLedger.class));
    }

    @Test
    @DisplayName("회원별 PaymentLedger 조회")
    void getPaymentLedgersByMember_Success() {
        // given
        Long memberId = 1L;
        List<PaymentLedger> ledgers = List.of(testPaymentLedger);
        
        given(paymentLedgerRepository.findByMemberIdOrderByCreatedAtDesc(memberId))
                .willReturn(ledgers);

        // when
        List<PaymentLedger> result = ledgerService.getPaymentLedgersByMember(memberId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMemberId()).isEqualTo(memberId);
        assertThat(result.get(0).getOrderId()).isEqualTo("ORDER_20240815_001");
        assertThat(result.get(0).getAmount()).isEqualTo(10000);
    }

    @Test
    @DisplayName("게시글별 PaymentLedger 조회")
    void getPaymentLedgersByPost_Success() {
        // given
        Long postId = 1L;
        List<PaymentLedger> ledgers = List.of(testPaymentLedger);
        
        given(paymentLedgerRepository.findByPostIdOrderByCreatedAtDesc(postId))
                .willReturn(ledgers);

        // when
        List<PaymentLedger> result = ledgerService.getPaymentLedgersByPost(postId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPostId()).isEqualTo(postId);
        assertThat(result.get(0).getOrderId()).isEqualTo("ORDER_20240815_001");
        assertThat(result.get(0).getAmount()).isEqualTo(10000);
    }

    @Test
    @DisplayName("회원별 PaymentLedger 조회 - 빈 결과")
    void getPaymentLedgersByMember_Empty() {
        // given
        Long memberId = 999L;
        given(paymentLedgerRepository.findByMemberIdOrderByCreatedAtDesc(memberId))
                .willReturn(List.of());

        // when
        List<PaymentLedger> result = ledgerService.getPaymentLedgersByMember(memberId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("게시글별 PaymentLedger 조회 - 빈 결과")
    void getPaymentLedgersByPost_Empty() {
        // given
        Long postId = 999L;
        given(paymentLedgerRepository.findByPostIdOrderByCreatedAtDesc(postId))
                .willReturn(List.of());

        // when
        List<PaymentLedger> result = ledgerService.getPaymentLedgersByPost(postId);

        // then
        assertThat(result).isEmpty();
    }
}