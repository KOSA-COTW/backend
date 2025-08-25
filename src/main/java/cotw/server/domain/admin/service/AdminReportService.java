package cotw.server.domain.admin.service;

import cotw.server.domain.admin.dto.request.AdminReportBulkActionRequest;
import cotw.server.domain.admin.dto.response.AdminReportDetailResponse;
import cotw.server.domain.admin.dto.response.AdminReportItemResponse;
import cotw.server.domain.admin.dto.response.AdminDashboardSummaryResponse;
import cotw.server.domain.comment.entity.ReportReason;
import cotw.server.domain.comment.repository.CommentReportRepository;
import cotw.server.domain.comment.repository.CommentRepository;
import cotw.server.domain.board.entity.Comment;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminReportService {

    private final CommentRepository commentRepo;
    private final CommentReportRepository reportRepo;

    // ===== 목록/상세 =====

    @Transactional(readOnly = true)
    public Page<AdminReportItemResponse> list(String status, ReportReason reason,
                                              LocalDateTime from, LocalDateTime to,
                                              Pageable pageable) {
        String s = status == null ? "ALL" : status.toUpperCase();
        return switch (s) {
            case "PENDING" -> commentRepo.findAdminPending(reason, from, to, pageable)
                    .map(this::toItem);
            case "EXPIRED" -> commentRepo.findAdminExpired(reason, from, to, pageable)
                    .map(this::toItem);
            case "HIDDEN" -> commentRepo.findAdminHidden(reason, from, to, pageable)
                    .map(this::toItem);
            default -> commentRepo.findAdminAll(reason, from, to, pageable)
                    .map(this::toItem);
        };
    }

    @Transactional(readOnly = true)
    public AdminReportDetailResponse detail(Long commentId) {
        Comment c = get(commentId);
        Map<ReportReason, Long> breakdown = breakdown(commentId);
        LocalDateTime lastReportedAt = reportRepo.findLastReportedAt(commentId).orElse(null);
        return new AdminReportDetailResponse(
                c.getId(), c.getPost().getId(), c.getMember().getId(),
                c.getContent(), c.getReportCount(), !c.isPublic(),
                c.getModerationDueAt(), c.getCreatedAt(), c.getUpdatedAt(),
                lastReportedAt, breakdown
        );
    }

    // ===== 조치 =====

    public void restore(Long commentId) {
        Comment c = get(commentId);
        c.restoreByAdmin(); // 신고수 0, 공개 전환, 삭제/만료 초기화
    }

    public void softDelete(Long commentId) {
        Comment c = get(commentId);
        c.delete(); // 소프트 삭제 + 비공개
    }

    public void reset(Long commentId) {
        Comment c = get(commentId);
        c.makePublic();
        // 신고수 0으로 초기화 (로그는 유지).
        c.applyReportTally(0);
    }

    public void bulk(AdminReportBulkActionRequest req) {
        for (Long id : req.commentIds()) {
            switch (req.action()) {
                case RESTORE -> restore(id);
                case DELETE -> softDelete(id);
                case RESET -> reset(id);
            }
        }
    }

    // ===== 대시보드 요약 =====

    @Transactional(readOnly = true)
    public AdminDashboardSummaryResponse summary() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        long todayReports = reportRepo.countByCreatedAtBetween(start, end);
        long pendingHidden = commentRepo.countAdminPending();
        long expiredHidden = commentRepo.countAdminExpired();
        long totalComments = commentRepo.count();
        return new AdminDashboardSummaryResponse(todayReports, pendingHidden, expiredHidden, totalComments);
    }

    // ===== 내부 유틸 =====

    private Comment get(Long id) {
        return commentRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("댓글을 찾을 수 없습니다: " + id));
    }

    private AdminReportItemResponse toItem(Comment c) {
        LocalDateTime lastReportedAt = reportRepo.findLastReportedAt(c.getId()).orElse(null);
        String preview = c.getContent();
        if (preview != null && preview.length() > 80) preview = preview.substring(0, 80) + "…";
        return new AdminReportItemResponse(
                c.getId(), c.getPost().getId(), c.getMember().getId(),
                preview, c.getReportCount(), !c.isPublic(), c.getModerationDueAt(),
                c.getCreatedAt(), lastReportedAt
        );
    }

    private Map<ReportReason, Long> breakdown(Long commentId) {
        List<Object[]> rows = reportRepo.countByReason(commentId);
        Map<ReportReason, Long> map = new EnumMap<>(ReportReason.class);
        for (Object[] r : rows) {
            ReportReason reason = (ReportReason) r[0];
            Long cnt = (Long) r[1];
            map.put(reason, cnt);
        }
        return map;
    }
}