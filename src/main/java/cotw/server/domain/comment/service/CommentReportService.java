package cotw.server.domain.comment.service;

import cotw.server.domain.comment.dto.request.ReportRequest;
import cotw.server.domain.comment.dto.response.ReportResponse;
import cotw.server.domain.comment.entity.Comment;
import cotw.server.domain.comment.entity.CommentReport;
import cotw.server.domain.comment.repository.CommentReportRepository;
import cotw.server.domain.comment.repository.CommentRepository;
import cotw.server.domain.member.entity.Member;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentReportService {

    private final CommentRepository commentRepository;
    private final CommentReportRepository reportRepository;
    private final EntityManager em;

    /**
     * 신고: 사유 필수, 하루 3회 제한, 중복 신고 방지,
     * 누적 3회 도달 시 isPublic=false + moderationDueAt=now+48h
     */
    public ReportResponse report(Long commentId, ReportRequest req) {
        Comment c = get(commentId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.toLocalDate().atStartOfDay();
        LocalDateTime end   = start.plusDays(1);

        long today = reportRepository.countTodayByMember(req.memberId(), start, end);
        if (today >= 3) throw new IllegalStateException("하루 신고 한도를 초과했습니다.");

        if (!reportRepository.existsByCommentIdAndMemberId(commentId, req.memberId())) {
            reportRepository.save(CommentReport.builder()
                    .comment(c)
                    .member(em.getReference(Member.class, req.memberId()))
                    .reason(req.reason())
                    .build());

            c.setReportCount(c.getReportCount() + 1);

            if (c.getReportCount() >= 3 && c.isPublic()) {
                c.setPublic(false);
                c.setModerationDueAt(now.plusHours(48));
            }
        }

        return new ReportResponse(c.getId(), c.getReportCount(), c.isPublic(), c.getModerationDueAt());
    }

    /** 관리자: 신고 초기화 */
    public void adminResetReports(Long commentId) {
        Comment c = get(commentId);
        c.setReportCount(0);
        c.setPublic(true);
        c.setModerationDueAt(null);
    }

    /** 관리자: 임시 숨김 해제(복원) */
    public void adminRestoreHidden(Long commentId) {
        Comment c = get(commentId);
        c.setPublic(true);
        c.setModerationDueAt(null);
    }

    private Comment get(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("comment not found: " + id));
    }
}
