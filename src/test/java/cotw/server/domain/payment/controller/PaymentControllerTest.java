package cotw.server.domain.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import cotw.server.domain.payment.config.SecurityUtil;
import cotw.server.domain.payment.dto.request.PaymentConfirmRequest;
import cotw.server.domain.payment.dto.request.PaymentCreateRequest;
import cotw.server.domain.payment.dto.response.PaymentCreateResponse;
import cotw.server.domain.payment.entity.PaymentLedger;
import cotw.server.domain.payment.entity.PaymentStatus;
import cotw.server.domain.payment.entity.PaymentType;
import cotw.server.domain.payment.exception.PaymentException;
import cotw.server.domain.payment.service.PaymentService;
import cotw.server.domain.payment.service.LedgerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.context.annotation.Import;

@WebMvcTest(PaymentController.class)
@Import(SecurityTestConfig.class)
@DisplayName("PaymentController 테스트")
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PaymentService paymentService;
    
    @MockitoBean
    private LedgerService ledgerService;

    private PaymentCreateRequest createRequest;
    private PaymentCreateResponse createResponse;
    private PaymentConfirmRequest confirmRequest;
    private PaymentLedger paymentLedger;

    @BeforeEach
    void setUp() {
        createRequest = new PaymentCreateRequest();
        setField(createRequest, "postId", 1L);
        setField(createRequest, "amount", 10000);

        createResponse = PaymentCreateResponse.builder()
                .orderId("ORDER_20240815_001")
                .amount(10000)
                .orderName("테스트 모금 게시글")
                .build();

        confirmRequest = PaymentConfirmRequest.builder()
                .paymentKey("payment_key_123")
                .orderId("ORDER_20240815_001")
                .amount(10000)
                .build();


        paymentLedger = PaymentLedger.builder()
                .id(1L)
                .orderId("ORDER_20240815_001")
                .paymentKey("payment_key_123")
                .memberName("테스트 사용자")
                .postTitle("테스트 모금 게시글")
                .memberId(1L)
                .postId(1L)
                .amount(10000)
                .status(PaymentStatus.DONE)
                .type(PaymentType.NORMAL)
                .originalCreatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("결제 생성 API - 성공")
    void createPayment_Success() throws Exception {
        // given
        Long memberId = 1L;
        
        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(memberId);
            given(paymentService.createPayment(any(PaymentCreateRequest.class), eq(memberId)))
                    .willReturn(createResponse);

            // when & then
            mockMvc.perform(post("/api/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createRequest)))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.orderId").value("ORDER_20240815_001"))
                    .andExpect(jsonPath("$.amount").value(10000))
                    .andExpect(jsonPath("$.orderName").value("테스트 모금 게시글"));
        }
    }

    @Test
    @DisplayName("결제 생성 API - 유효하지 않은 요청")
    void createPayment_InvalidRequest() throws Exception {
        // given
        PaymentCreateRequest invalidRequest = new PaymentCreateRequest();
        setField(invalidRequest, "postId", null);
        setField(invalidRequest, "amount", -1000);

        Long memberId = 1L;

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(memberId);
            given(paymentService.createPayment(any(PaymentCreateRequest.class), eq(memberId)))
                    .willThrow(new PaymentException("유효하지 않은 요청입니다."));

            // when & then
            mockMvc.perform(post("/api/payments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }


    @Test
    @DisplayName("결제 성공 콜백 API - 성공")
    void paymentSuccess_Success() throws Exception {
        // given

        // when & then
        mockMvc.perform(get("/api/payments/success")
                        .param("orderId", "ORDER_20240815_001")
                        .param("paymentKey", "payment_key_123")
                        .param("amount", "10000"))
                .andDo(print())
                .andExpect(status().isFound())
                .andExpect(header().string("Location", 
                        "http://localhost:5173/payment/success?orderId=ORDER_20240815_001"));
    }

    @Test
    @DisplayName("결제 성공 콜백 API - 실패")
    void paymentSuccess_Failure() throws Exception {
        // given
        willThrow(new PaymentException("결제 승인에 실패했습니다."))
                .given(paymentService).confirmPayment(any(PaymentConfirmRequest.class));

        // when & then
        mockMvc.perform(get("/api/payments/success")
                        .param("orderId", "ORDER_20240815_001")
                        .param("paymentKey", "payment_key_123")
                        .param("amount", "10000"))
                .andDo(print())
                .andExpect(status().isFound())
                .andExpect(header().string("Location", 
                        "http://localhost:5173/payment/fail?error=payment_failed"));
    }

    @Test
    @DisplayName("회원별 결제 내역 조회 API - 성공")
    void getPaymentsByMember_Success() throws Exception {
        // given
        Long memberId = 1L;
        List<PaymentLedger> ledgers = List.of(paymentLedger);
        
        given(ledgerService.getPaymentLedgersByMember(memberId))
                .willReturn(ledgers);

        // when & then
        mockMvc.perform(get("/api/payments/member/{memberId}", memberId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].orderId").value("ORDER_20240815_001"))
                .andExpect(jsonPath("$[0].amount").value(10000));
    }

    @Test
    @DisplayName("게시글별 결제 내역 조회 API - 성공")
    void getPaymentsByPost_Success() throws Exception {
        // given
        Long postId = 1L;
        List<PaymentLedger> ledgers = List.of(paymentLedger);
        
        given(ledgerService.getPaymentLedgersByPost(postId))
                .willReturn(ledgers);

        // when & then
        mockMvc.perform(get("/api/payments/post/{postId}", postId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].orderId").value("ORDER_20240815_001"))
                .andExpect(jsonPath("$[0].amount").value(10000));
    }

    @Test
    @DisplayName("내 결제 내역 조회 API - 성공")
    void getMyPayments_Success() throws Exception {
        // given
        Long memberId = 1L;
        List<PaymentLedger> ledgers = List.of(paymentLedger);

        try (MockedStatic<SecurityUtil> mockedSecurityUtil = mockStatic(SecurityUtil.class)) {
            mockedSecurityUtil.when(SecurityUtil::getCurrentMemberId).thenReturn(memberId);
            given(ledgerService.getPaymentLedgersByMember(memberId))
                    .willReturn(ledgers);

            // when & then
            mockMvc.perform(get("/api/payments/my"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].orderId").value("ORDER_20240815_001"))
                    .andExpect(jsonPath("$[0].memberName").value("테스트 사용자"));
        }
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