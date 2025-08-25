package cotw.server.domain.comment.service;

import cotw.server.domain.board.entity.Comment;
import cotw.server.domain.comment.dto.request.ReportRequest;
import cotw.server.domain.comment.dto.response.ReportResponse;
import cotw.server.domain.comment.entity.CommentReport;
import cotw.server.domain.comment.entity.ReportReason;
import cotw.server.domain.comment.repository.CommentReportRepository;
import cotw.server.domain.comment.repository.CommentRepository;
import cotw.server.domain.member.entity.Member;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
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
        // 1) 하루 신고 3회 제한 ([start, end)로 집계)
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        if (reportRepository.countDailyByMember(reporterId, start, end) >= DAILY_LIMIT) {
            throw new IllegalStateException("하루 신고 한도(3회)를 초과했습니다.");
        }

        // 2) ETC면 detail 필수
        if (req.reason() == ReportReason.ETC &&
                (req.detail() == null || req.detail().isBlank())) {
            throw new IllegalStateException("기타 사유를 선택한 경우 상세 내용을 입력해주세요.");
        }

        // 3) 신고 대상 댓글 로딩 및 검증
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("댓글을 찾을 수 없습니다: " + commentId));
        if (comment.isDeleted()) {
            throw new IllegalStateException("삭제된 댓글은 신고할 수 없습니다.");
        }
        if (!comment.isPublic()) { // 숨김 처리된 댓글 신고 금지(정책)
            throw new IllegalStateException("이미 숨겨진 댓글은 신고할 수 없습니다.");
        }
        if (comment.getMember().getId().equals(reporterId)) { // 본인 댓글 신고 금지(정책)
            throw new IllegalStateException("본인 댓글은 신고할 수 없습니다.");
        }

        // 프록시 참조(성능): 신고자 엔티티 전체 로딩 대신 프록시로 연관관계 세팅
        Member reporterRef = em.getReference(Member.class, reporterId);

        // 4) 신고 저장(UNIQUE 제약으로 중복 레이스 방어)
        String detail = (req.detail() == null || req.detail().isBlank()) ? null : req.detail().trim();
        try {
            reportRepository.save(CommentReport.builder()
                    .comment(comment)
                    .member(reporterRef)
                    .reason(req.reason())
                    .detail(detail)
                    .build());
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 사용자가 같은 댓글을 두 번 누른 경우 등
            throw new IllegalStateException("이미 신고한 댓글입니다.");
        }

        // 5) 저장 후 총 신고 수 재계산 → 엔티티 규칙으로 일관 반영
        int total = (int) reportRepository.countByCommentId(commentId);
        comment.applyReportTally(total);

        // 6) 응답(현재 상태 스냅샷)
        return new ReportResponse(
                commentId,
                reporterId,
                req.reason(),
                comment.getReportCount(),
                !comment.isPublic(),            // hidden = !isPublic
                comment.getModerationDueAt()
        );
    }
}
