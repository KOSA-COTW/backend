package cotw.server.domain.admin.service;

import cotw.server.domain.admin.dto.response.*;
import cotw.server.domain.comment.repository.CommentReportRepository;
import cotw.server.domain.comment.repository.CommentRepository;
import cotw.server.domain.member.repository.MemberRepository;
import cotw.server.domain.payment.entity.PaymentStatus;
import cotw.server.domain.payment.repository.PaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final CommentReportRepository commentReportRepository;
    private final CommentRepository commentRepository;   // ✅ 댓글 기준 집계
    private final MemberRepository memberRepository;

    public AdminDashboardResponse getDashboard(LocalDate start, LocalDate end) {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();

        LocalDate s = (start != null) ? start : today.withDayOfMonth(1);
        LocalDate e = (end != null) ? end : today;
        LocalDateTime from = s.atStartOfDay();
        LocalDateTime to = e.plusDays(1).atStartOfDay();

        // ✅ KPI 계산
        long todayDonation = paymentOrderRepository.sumDoneBetween(todayStart, todayEnd);
        long totalDonation = paymentOrderRepository.sumDoneBetween(from, to);

        long newUsers = memberRepository.countByCreatedAtBetween(todayStart, todayEnd);
        long totalUsers = memberRepository.countActiveMembers();

        // ✅ 미처리 신고 = 신고당한 댓글 수
        long pendingReports = commentRepository.countReportedComments();

        // ✅ 48시간 내 처리 대상 = 블라인드 처리된 댓글 수
        long dueIn48h = commentRepository.countDueIn48h(LocalDateTime.now().plusHours(48));

        var kpi = new AdminDashboardKpiResponse(
                todayDonation,
                totalDonation,
                newUsers,
                totalUsers,
                pendingReports,
                dueIn48h
        );

        // 최근 기부내역 5건 → 이메일 내려주기
        List<AdminRecentPaymentResponse> recentPayments = paymentOrderRepository
                .findTop5ByStatusOrderByCreatedAtDesc(PaymentStatus.DONE)
                .stream()
                .map(po -> new AdminRecentPaymentResponse(
                        po.getMember().getEmail(),   // ✅ 이메일
                        po.getAmount(),
                        po.getStatus().name(),
                        po.getCreatedAt()
                ))
                .toList();

        // 최근 신고 댓글 5건 → 댓글 작성자 이메일 내려주기
        List<Object[]> recent = commentReportRepository.findRecentCommentIds(PageRequest.of(0, 5));
        List<AdminRecentReportResponse> recentReports = recent.stream().map(r -> {
            Long commentId = (Long) r[0];
            LocalDateTime lastAt = (LocalDateTime) r[1];
            long count = commentReportRepository.countActiveByCommentId(commentId);

            String top = null;
            int max = -1;
            for (Object[] row : commentReportRepository.countActiveByReason(commentId)) {
                String reason = String.valueOf(row[0]);
                int c = ((Number) row[1]).intValue();
                if (c > max) { max = c; top = reason; }
            }

            // ✅ 댓글 작성자 이메일 가져오기
            String reportedMemberEmail = commentReportRepository
                    .findFirstByCommentIdOrderByCreatedAtAsc(commentId)
                    .getComment()
                    .getMember()
                    .getEmail();

            return new AdminRecentReportResponse(reportedMemberEmail, top, count, lastAt);
        }).toList();

        // 일간 기부액 추이
        Map<LocalDate, Long> dailyMap = new HashMap<>();
        for (Object[] row : paymentOrderRepository.sumDailyBetween(from, to)) {
            LocalDate day = ((java.sql.Date) row[0]).toLocalDate(); // ✅ 항상 java.sql.Date로 변환
            dailyMap.put(day, ((Number) row[1]).longValue());
        }
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        LocalDate cursor = s;
        while (!cursor.isAfter(e)) {
            labels.add(cursor.toString());
            values.add(dailyMap.getOrDefault(cursor, 0L));
            cursor = cursor.plusDays(1);
        }
        var daily = new AdminDailyDonationResponse(labels, values);

        return new AdminDashboardResponse(kpi, recentPayments, recentReports, daily);
    }
}
