package cotw.server.domain.payment.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentLedger 엔티티 테스트")
class PaymentLedgerTest {

    @Test
    @DisplayName("PaymentLedger 생성 - 모든 필드 설정")
    void createPaymentLedger_AllFields() {
        // given & when
        PaymentLedger paymentLedger = PaymentLedger.builder()
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

        // then
        assertThat(paymentLedger.getOrderId()).isEqualTo("ORDER_20240815_001");
        assertThat(paymentLedger.getPaymentKey()).isEqualTo("payment_key_123");
        assertThat(paymentLedger.getMemberId()).isEqualTo(1L);
        assertThat(paymentLedger.getPostId()).isEqualTo(1L);
        assertThat(paymentLedger.getAmount()).isEqualTo(10000);
        assertThat(paymentLedger.getStatus()).isEqualTo(PaymentStatus.DONE);
        assertThat(paymentLedger.getType()).isEqualTo(PaymentType.NORMAL);
        assertThat(paymentLedger.getMemberName()).isEqualTo("테스트 사용자");
        assertThat(paymentLedger.getPostTitle()).isEqualTo("테스트 모금 게시글");
    }

    @Test
    @DisplayName("PaymentLedger 필수 필드 검증")
    void createPaymentLedger_RequiredFields() {
        // given & when
        PaymentLedger paymentLedger = PaymentLedger.builder()
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

        // then - 모든 필수 필드가 설정되어야 함
        assertThat(paymentLedger.getOrderId()).isNotNull();
        assertThat(paymentLedger.getPaymentKey()).isNotNull();
        assertThat(paymentLedger.getMemberId()).isNotNull();
        assertThat(paymentLedger.getPostId()).isNotNull();
        assertThat(paymentLedger.getAmount()).isNotNull();
        assertThat(paymentLedger.getStatus()).isNotNull();
        assertThat(paymentLedger.getType()).isNotNull();
        assertThat(paymentLedger.getMemberName()).isNotNull();
        assertThat(paymentLedger.getPostTitle()).isNotNull();
    }

    @Test
    @DisplayName("PaymentLedger - 결제 타입별 생성")
    void createPaymentLedger_DifferentTypes() {
        // given & when - NORMAL 타입
        PaymentLedger normalLedger = PaymentLedger.builder()
                .orderId("ORDER_NORMAL_001")
                .paymentKey("payment_key_normal")
                .memberId(1L)
                .postId(1L)
                .amount(10000)
                .status(PaymentStatus.DONE)
                .type(PaymentType.NORMAL)
                .memberName("일반 사용자")
                .postTitle("일반 모금")
                .build();

        // then
        assertThat(normalLedger.getType()).isEqualTo(PaymentType.NORMAL);
        assertThat(normalLedger.getOrderId()).isEqualTo("ORDER_NORMAL_001");

        // 다른 타입이 추가되면 여기서 테스트 가능
    }

    @Test
    @DisplayName("PaymentLedger - 결제 상태별 생성")
    void createPaymentLedger_DifferentStatuses() {
        // given & when - DONE 상태
        PaymentLedger doneLedger = PaymentLedger.builder()
                .orderId("ORDER_DONE_001")
                .paymentKey("payment_key_done")
                .memberId(1L)
                .postId(1L)
                .amount(10000)
                .status(PaymentStatus.DONE)
                .type(PaymentType.NORMAL)
                .memberName("완료 사용자")
                .postTitle("완료 모금")
                .build();

        // then
        assertThat(doneLedger.getStatus()).isEqualTo(PaymentStatus.DONE);
        assertThat(doneLedger.getOrderId()).isEqualTo("ORDER_DONE_001");

        // given & when - ABORTED 상태
        PaymentLedger abortedLedger = PaymentLedger.builder()
                .orderId("ORDER_ABORTED_001")
                .paymentKey("payment_key_aborted")
                .memberId(1L)
                .postId(1L)
                .amount(10000)
                .status(PaymentStatus.ABORTED)
                .type(PaymentType.NORMAL)
                .memberName("실패 사용자")
                .postTitle("실패 모금")
                .build();

        // then
        assertThat(abortedLedger.getStatus()).isEqualTo(PaymentStatus.ABORTED);
        assertThat(abortedLedger.getOrderId()).isEqualTo("ORDER_ABORTED_001");
    }
}