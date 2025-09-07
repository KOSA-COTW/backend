package cotw.server.domain.admin.service;

import cotw.server.domain.admin.dto.response.AdminCategoryStatsResponse;
import cotw.server.domain.admin.dto.response.AdminDonationStatsResponse;
import cotw.server.domain.admin.dto.response.AdminTopDonorResponse;
import cotw.server.domain.board.entity.Category;
import cotw.server.domain.payment.repository.PaymentLedgerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminStatsService {

    private final PaymentLedgerRepository paymentLedgerRepository;

    /** 총 기부 금액 + 상위 10명 (PaymentLedger 기준) */
    public AdminDonationStatsResponse getDonationStats() {
        long total = paymentLedgerRepository.sumDoneAll();
        List<Object[]> results = paymentLedgerRepository.findTopDonorsByAmount(10);
        
        List<AdminTopDonorResponse> topDonors = results.stream()
                .map(result -> new AdminTopDonorResponse(
                        (Long) result[0],       // memberId
                        (String) result[1],     // memberName
                        ((Number) result[2]).longValue()  // totalAmount
                ))
                .toList();
        
        return new AdminDonationStatsResponse(total, topDonors);
    }

    /** 카테고리별 기부금액 순위 */
    public List<AdminCategoryStatsResponse> getCategoryStats() {
        List<Object[]> results = paymentLedgerRepository.findDonationAmountByCategory();
        
        return results.stream()
                .map(result -> new AdminCategoryStatsResponse(
                        (Category) result[0],   // category
                        ((Category) result[0]).getDisplayName(),  // categoryName
                        ((Number) result[1]).longValue()  // totalAmount
                ))
                .toList();
    }
}
