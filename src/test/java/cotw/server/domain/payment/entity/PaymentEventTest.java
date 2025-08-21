package cotw.server.domain.payment.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PaymentEvent 엔티티 테스트")
class PaymentEventTest {

    @Test
    @DisplayName("PaymentEvent 생성 - 기본값 확인")
    void createPaymentEvent_DefaultValues() {
        // given & when
        PaymentEvent paymentEvent = PaymentEvent.builder()
                .orderId("ORDER_20240815_001")
                .memberId(1L)
                .postId(1L)
                .amount(10000)
                .build();

        // then
        assertThat(paymentEvent.getOrderId()).isEqualTo("ORDER_20240815_001");
        assertThat(paymentEvent.getMemberId()).isEqualTo(1L);
        assertThat(paymentEvent.getPostId()).isEqualTo(1L);
        assertThat(paymentEvent.getAmount()).isEqualTo(10000);
        assertThat(paymentEvent.getStatus()).isEqualTo(PaymentStatus.READY); // 기본값
        assertThat(paymentEvent.getType()).isEqualTo(PaymentType.NORMAL); // 기본값
    }

    @Test
    @DisplayName("PaymentEvent 생성 - 모든 필드 설정")
    void createPaymentEvent_AllFields() {
        // given & when
        PaymentEvent paymentEvent = PaymentEvent.builder()
                .orderId("ORDER_20240815_001")
                .memberId(1L)
                .postId(1L)
                .amount(10000)
                .status(PaymentStatus.DONE)
                .type(PaymentType.NORMAL)
                .build();

        // then
        assertThat(paymentEvent.getOrderId()).isEqualTo("ORDER_20240815_001");
        assertThat(paymentEvent.getMemberId()).isEqualTo(1L);
        assertThat(paymentEvent.getPostId()).isEqualTo(1L);
        assertThat(paymentEvent.getAmount()).isEqualTo(10000);
        assertThat(paymentEvent.getStatus()).isEqualTo(PaymentStatus.DONE);
        assertThat(paymentEvent.getType()).isEqualTo(PaymentType.NORMAL);
    }

    @Test
    @DisplayName("결제 상태 업데이트")
    void updateStatus() {
        // given
        PaymentEvent paymentEvent = PaymentEvent.builder()
                .orderId("ORDER_20240815_001")
                .memberId(1L)
                .postId(1L)
                .amount(10000)
                .build();

        assertThat(paymentEvent.getStatus()).isEqualTo(PaymentStatus.READY);

        // when
        paymentEvent.updateStatus(PaymentStatus.DONE);

        // then
        assertThat(paymentEvent.getStatus()).isEqualTo(PaymentStatus.DONE);
    }

    @Test
    @DisplayName("결제 상태 변경 - READY에서 DONE으로")
    void updateStatus_ReadyToDone() {
        // given
        PaymentEvent paymentEvent = PaymentEvent.builder()
                .orderId("ORDER_20240815_001")
                .memberId(1L)
                .postId(1L)
                .amount(10000)
                .status(PaymentStatus.READY)
                .build();

        // when
        paymentEvent.updateStatus(PaymentStatus.DONE);

        // then
        assertThat(paymentEvent.getStatus()).isEqualTo(PaymentStatus.DONE);
    }

    @Test
    @DisplayName("결제 상태 변경 - READY에서 ABORTED로")
    void updateStatus_ReadyToAborted() {
        // given
        PaymentEvent paymentEvent = PaymentEvent.builder()
                .orderId("ORDER_20240815_001")
                .memberId(1L)
                .postId(1L)
                .amount(10000)
                .status(PaymentStatus.READY)
                .build();

        // when
        paymentEvent.updateStatus(PaymentStatus.ABORTED);

        // then
        assertThat(paymentEvent.getStatus()).isEqualTo(PaymentStatus.ABORTED);
    }
}