package cotw.server.domain.comment.service;

import cotw.server.domain.board.entity.Comment;
import cotw.server.domain.comment.dto.request.ReportRequest;
import cotw.server.domain.comment.dto.response.ReportResponse;
import cotw.server.domain.comment.entity.CommentReport;
import cotw.server.domain.comment.entity.ReportReason;
import cotw.server.domain.comment.repository.CommentReportRepository;
import cotw.server.domain.comment.repository.CommentRepository;
import cotw.server.domain.member.entity.Member;
import cotw.server.domain.comment.exception.CommentException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentReportService {

    private static final int DAILY_LIMIT = 3;

    private final CommentRepository commentRepository;
    private final CommentReportRepository reportRepository;
    private final EntityManager em;

    public ReportResponse report(Long reporterId, Long commentId, ReportRequest req) {
        // 1) 하루 신고 3회 제한
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        if (reportRepository.countDailyByMember(reporterId, start, end) >= DAILY_LIMIT) {
            throw new CommentException("하루 신고 한도(3회)를 초과했습니다.", "REPORT_DAILY_LIMIT");
        }

        // 2) 기타 사유면 상세 필수
        if (req.reason() == ReportReason.ETC &&
                (req.detail() == null || req.detail().isBlank())) {
            throw new CommentException("기타 사유를 선택한 경우 상세 내용을 입력해주세요.", "REPORT_DETAIL_REQUIRED");
        }

        // 3) 대상 댓글 검증
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CommentException("댓글을 찾을 수 없습니다: " + commentId, "COMMENT_NOT_FOUND"));
        if (comment.isDeleted()) {
            throw new CommentException("삭제된 댓글은 신고할 수 없습니다.", "COMMENT_DELETED");
        }
        if (!comment.isPublic()) {
            throw new CommentException("이미 숨겨진 댓글은 신고할 수 없습니다.", "COMMENT_HIDDEN");
        }
        if (comment.getMember().getId().equals(reporterId)) {
            throw new CommentException("본인 댓글은 신고할 수 없습니다.", "CANNOT_REPORT_OWN_COMMENT");
        }

        // 4) 신고 저장 (UNIQUE 제약으로 중복 방지)
        String detail = (req.detail() == null || req.detail().isBlank()) ? null : req.detail().trim();
        try {
            reportRepository.save(CommentReport.builder()
                    .comment(comment) // 관리 상태 엔티티
                    .member(em.getReference(Member.class, reporterId))
                    .reason(req.reason())
                    .detail(detail)
                    .build());
        } catch (DataIntegrityViolationException e) {
            throw new CommentException("이미 신고한 댓글입니다.", "ALREADY_REPORTED");
        }

        // 5) 총 신고 수 재계산 및 규칙 반영
        int total = (int) reportRepository.countActiveByCommentId(commentId);
        comment.applyReportTally(total);

        // 최신 스냅샷 보장
        em.flush();
        em.refresh(comment);

        // 6) 응답
        return new ReportResponse(
                commentId,
                reporterId,
                req.reason(),
                comment.getReportCount(),
                !comment.isPublic(),
                comment.getModerationDueAt()
        );
    }
}