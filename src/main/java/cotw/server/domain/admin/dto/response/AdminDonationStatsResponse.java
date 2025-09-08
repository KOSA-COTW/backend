package cotw.server.domain.admin.dto.response;

import java.util.List;

/** 총 기부액 + 상위 10명 묶음 */
public record AdminDonationStatsResponse(
        long totalAmount,                       //
        List<AdminTopDonorResponse> topDonors
) {}