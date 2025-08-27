package cotw.server.domain.admin.dto.response;

/** 상위 기부자 응답 (PaymentOrder 기준) */
public record AdminTopDonorResponse(
        Long memberId,
        String memberName,
        long totalAmount
) {}
