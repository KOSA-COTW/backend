package cotw.server.domain.admin.service;

import cotw.server.domain.admin.dto.response.*;
import cotw.server.domain.payment.entity.PaymentStatus;
import cotw.server.domain.payment.repository.PaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDonationService {

    private final PaymentOrderRepository paymentOrderRepository;

    public AdminDashboardStatsResponse getDashboardStats() {
        long totalAmount = paymentOrderRepository.sumAllDone();
        long totalDonations = paymentOrderRepository.countByStatus(PaymentStatus.DONE);
        long totalMembers = paymentOrderRepository.countDistinctMemberByStatus(PaymentStatus.DONE);
        return new AdminDashboardStatsResponse(totalAmount, totalDonations, totalMembers);
    }

    public List<AdminTopDonorDashboardResponse> getTopDonors(int limit) {
        List<AdminTopDonorProjection> raw = paymentOrderRepository.findTopDonorProjections(PageRequest.of(0, limit));
        AtomicInteger rank = new AtomicInteger(1);
        return raw.stream()
                .map(r -> new AdminTopDonorDashboardResponse(
                        rank.getAndIncrement(),
                        r.memberName(),
                        r.memberEmail(),
                        r.totalAmount(),
                        r.donationCount(),
                        r.lastDonationDate()))
                .toList();
    }

    public AdminDonationListResponse getDonations(int page, int size, String search, String status) {
        PaymentStatus paymentStatus = parseStatus(status);

        // ✅ 여기서 % ... % 를 미리 만들고 소문자로 통일
        String keyword = (search != null && !search.isBlank())
                ? "%" + search.trim().toLowerCase() + "%"
                : null;

        Page<AdminDonationListItemResponse> result =
                paymentOrderRepository.searchDonations(keyword, paymentStatus, PageRequest.of(page - 1, size));
        return new AdminDonationListResponse(result.getContent(), result.getTotalElements());
    }

    private PaymentStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return PaymentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
