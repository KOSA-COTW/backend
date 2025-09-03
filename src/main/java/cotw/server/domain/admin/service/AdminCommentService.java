package cotw.server.domain.admin.service;

import cotw.server.domain.admin.dto.request.CommentListRequest;
import cotw.server.domain.admin.dto.response.CommentRowResponse;
import cotw.server.domain.admin.dto.response.ReportLogResponse;
import cotw.server.domain.board.entity.Post;
import cotw.server.domain.board.entity.Comment;
import cotw.server.domain.comment.entity.CommentReport;
import cotw.server.domain.comment.entity.ReportReason;
import cotw.server.domain.comment.repository.CommentReportRepository;
import cotw.server.domain.comment.repository.CommentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCommentService {

    private final CommentRepository commentRepo;
    private final CommentReportRepository reportRepo;
    private final EntityManager em;

    public Page<CommentRowResponse> list(CommentListRequest q, Pageable pageable) {
        // 동적 JPQL (검색/필터/정렬)
        StringBuilder jpql = new StringBuilder("""
            select c from Comment c
            left join fetch c.member m
            left join fetch c.post p
            where 1=1
        """);
        Map<String,Object> params = new HashMap<>();

        if (q.getKeyword() != null && !q.getKeyword().isBlank()) {
            jpql.append(" and ( lower(c.content) like :kw or lower(m.email) like :kw or lower(p.title) like :kw )");
            params.put("kw", "%"+q.getKeyword().toLowerCase()+"%");
        }
        if (q.getReason() != null) {
            jpql.append("""
                and exists (
                  select r.id from CommentReport r
                  where r.comment = c and r.reason = :reason
                )
            """);
            params.put("reason", q.getReason());
        }
        if (q.getMinReports() != null && q.getMinReports() > 0) {
            jpql.append(" and (c.reportCount >= :minReports)");
            params.put("minReports", q.getMinReports());
        }
        if (q.getStart() != null) {
            jpql.append(" and c.createdAt >= :start");
            params.put("start", q.getStart());
        }
        if (q.getEnd() != null) {
            jpql.append(" and c.createdAt <= :end");
            params.put("end", q.getEnd());
        }

        // 상태 필터
        switch (Optional.ofNullable(q.getStatus()).orElse("ALL")) {
            case "HIDDEN" -> jpql.append(" and c.isPublic = false");
            case "PENDING" -> {
                jpql.append(" and c.moderationDueAt is not null and c.moderationDueAt >= :now");
                params.put("now", LocalDateTime.now());
            }
            case "EXPIRED" -> {
                jpql.append(" and c.moderationDueAt is not null and c.moderationDueAt < :now");
                params.put("now", LocalDateTime.now());
            }
            default -> {}
        }

        boolean reportedOnly = Boolean.TRUE.equals(q.getReportedOnly()) || q.isOnlyPending();
        if (reportedOnly) {
            jpql.append(" and c.reportCount > 0");
        }



        // 정렬
        String orderBy = switch (Optional.ofNullable(q.getSort()).orElse("REPORT_DESC")) {
            case "DATE_ASC" -> " c.createdAt asc, c.id asc ";
            case "DATE_DESC" -> " c.createdAt desc, c.id desc ";
            default -> " c.reportCount desc, c.id desc "; // REPORT_DESC
        };
        jpql.append(" order by ").append(orderBy);

        // 조회 + 페이징(간단 버전: 전체 조회 후 subList)
        TypedQuery<Comment> query = em.createQuery(jpql.toString(), Comment.class);
        params.forEach(query::setParameter);
        List<Comment> all = query.getResultList();

        int from = (int) pageable.getOffset();
        if (from >= all.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, all.size());
        }
        int to = Math.min(from + pageable.getPageSize(), all.size());
        List<CommentRowResponse> pageRows = all.subList(from, to).stream()
                .map(this::toRow)
                .toList();

        return new PageImpl<>(pageRows, pageable, all.size());
    }

    private CommentRowResponse toRow(Comment c) {
        String author = c.getMember() != null ? c.getMember().getEmail() : null;
        String postTitle = Optional.ofNullable(c.getPost()).map(Post::getTitle).orElse(null);

        // topReason: 신고 사유 최다 1개 (람다 캡처 없이 계산)
        String topReason = null;
        if (c.getReportCount() != 0) {
            List<Object[]> byReason = reportRepo.countByReason(c.getId());
            topReason = byReason.stream()
                    .max(Comparator.comparingLong(o -> ((Number) o[1]).longValue()))
                    .map(o -> ((ReportReason) o[0]).name())
                    .orElse(null);
        }

        // moderationDueAt: 엔티티 값 우선, 없고 신고가 있으면 최초 신고 +48h로 보완 계산
        var due = c.getModerationDueAt();
        if (due == null && c.getReportCount() > 0) {
            var first = reportRepo.findFirstByCommentIdOrderByCreatedAtAsc(c.getId());
            if (first != null) due = first.getCreatedAt().plusHours(48);
        }

        return new CommentRowResponse(
                c.getId(),
                c.getContent(),
                author,
                postTitle,
                c.isPublic(),
                c.getReportCount(),
                topReason,
                c.getCreatedAt(),
                due
        );
    }

    public List<ReportLogResponse> reportLogs(Long commentId) {
        // ✅ 레포지토리 메서드로 대체 (N+1 방지 위해 @EntityGraph 붙여둔 버전 권장)
        List<CommentReport> logs = reportRepo.findByCommentIdOrderByCreatedAtAsc(commentId);

        return logs.stream()
                .map(r -> new ReportLogResponse(
                        r.getReason().name(),
                        r.getMember()!=null ? r.getMember().getEmail() : "익명",
                        r.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void setVisibility(Long id, boolean visible) {
        Comment c = commentRepo.findById(id).orElseThrow();
        c.setPublic(visible);
        if (visible) c.setModerationDueAt(null);
    }

    @Transactional
    public void resetReports(Long id) {
        Comment c = commentRepo.findById(id).orElseThrow();
        reportRepo.deleteByCommentId(id);
        c.setReportCount(0);
        c.setPublic(true);
        c.setModerationDueAt(null);
    }

    @Transactional
    public void delete(Long id) {
        reportRepo.deleteByCommentId(id);
        commentRepo.deleteById(id);
    }

    @Transactional
    public void saveMemo(Long id, String memo) {
        Comment c = commentRepo.findById(id).orElseThrow();
        c.setAdminMemo(memo); // Comment 엔티티에 adminMemo 필드 필요
    }

    @Transactional
    public void bulkVisibility(List<Long> ids, boolean visible) {
        if (ids == null || ids.isEmpty()) return;
        commentRepo.findAllById(ids).forEach(c -> {
            c.setPublic(visible);
            if (visible) c.setModerationDueAt(null);
        });
    }

    @Transactional
    public void bulkResetReports(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        // ✅ 신고 로그 일괄 삭제 최적화
        reportRepo.deleteByCommentIdIn(ids);
        commentRepo.findAllById(ids).forEach(c -> {
            c.setReportCount(0);
            c.setPublic(true);
            c.setModerationDueAt(null);
        });
    }

    @Transactional
    public void bulkDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        // ✅ 신고 로그 일괄 삭제 후 댓글 일괄 삭제
        reportRepo.deleteByCommentIdIn(ids);
        commentRepo.deleteAllById(ids);
    }
}
