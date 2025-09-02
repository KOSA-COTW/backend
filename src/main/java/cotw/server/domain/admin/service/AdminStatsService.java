package cotw.server.domain.admin.service;

import cotw.server.domain.admin.dto.response.AdminDonationStatsResponse;
import cotw.server.domain.admin.dto.response.AdminTopDonorResponse;
import cotw.server.domain.payment.repository.PaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatsService {

    private final PaymentOrderRepository paymentOrderRepository;

    /** 총 기부 금액 + 상위 10명 */
    public AdminDonationStatsResponse getDonationStats() {
        long total = paymentOrderRepository.sumAllDone();
        List<AdminTopDonorResponse> top = paymentOrderRepository.findTopDonors(10);
        return new AdminDonationStatsResponse(total, top);
    }
}
