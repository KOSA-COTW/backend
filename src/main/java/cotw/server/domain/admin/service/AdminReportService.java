package cotw.server.domain.admin.service;

import cotw.server.domain.admin.dto.request.AdminCommentSearchRequest;
import cotw.server.domain.admin.dto.response.AdminCommentRowResponse;
import cotw.server.domain.admin.dto.response.AdminReportLogResponse;
import cotw.server.domain.board.entity.Comment;
import cotw.server.domain.comment.entity.ReportReason;
import cotw.server.domain.comment.repository.CommentReportRepository;
import cotw.server.domain.comment.repository.CommentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminReportService {

    private final CommentRepository commentRepository;
    private final CommentReportRepository reportRepository;

    // ===== 목록 =====
    @Transactional(readOnly = true)
    public Page<AdminCommentRowResponse> list(AdminCommentSearchRequest q) {
        int page = Math.max(1, Optional.ofNullable(q.getPage()).orElse(1)) - 1; // 0-base
        int size = Math.max(1, Optional.ofNullable(q.getSize()).orElse(10));

        Sort sort = switch (Optional.ofNullable(q.getSort()).orElse("REPORT_DESC")) {
            case "DATE_ASC"  -> Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));
            case "DATE_DESC" -> Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
            default          -> Sort.by(Sort.Order.desc("reportCount"), Sort.Order.desc("id"));
        };
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Comment> spec = specFrom(q);
        return commentRepository.findAll(spec, pageable)
                .map(this::toRow);
    }

    private Specification<Comment> specFrom(AdminCommentSearchRequest q) {
        return (root, cq, cb) -> {
            List<jakarta.persistence.criteria.Predicate> ps = new ArrayList<>();

            // ✅ 공통: 삭제된 댓글 제외
            ps.add(cb.isNull(root.get("deletedAt")));

            // HIDDEN / EXPIRED / REPORTED / ALL / onlyPending
            String status = Optional.ofNullable(q.getStatus()).orElse("ALL");
            switch (status) {
                case "HIDDEN" -> ps.add(cb.isFalse(root.get("isPublic")));
                case "EXPIRED" -> {
                    ps.add(cb.isFalse(root.get("isPublic")));
                    ps.add(cb.isNotNull(root.get("moderationDueAt")));
                    ps.add(cb.lessThan(root.get("moderationDueAt"), LocalDateTime.now()));
                }
                case "REPORTED" -> {
                    // ✅ 활성 신고만 기준으로 필터링
                    var sub = cq.subquery(Long.class);
                    var r = sub.from(cotw.server.domain.comment.entity.CommentReport.class);
                    sub.select(cb.literal(1L));
                    sub.where(
                            cb.equal(r.get("comment").get("id"), root.get("id")),
                            cb.isNull(r.get("clearedAt"))
                    );
                    ps.add(cb.exists(sub));
                }
                default -> {} // ALL
            }

            if (Boolean.TRUE.equals(q.getOnlyPending())) {
                ps.add(cb.isFalse(root.get("isPublic")));
                ps.add(cb.isNotNull(root.get("moderationDueAt")));
                ps.add(cb.greaterThanOrEqualTo(root.get("moderationDueAt"), LocalDateTime.now()));
            }

            // minReports
            Integer min = Optional.ofNullable(q.getMinReports()).orElse(0);
            if (min > 0) {
                var sub = cq.subquery(Long.class);
                var r = sub.from(cotw.server.domain.comment.entity.CommentReport.class);
                sub.select(cb.count(r));
                sub.where(
                        cb.equal(r.get("comment").get("id"), root.get("id")),
                        cb.isNull(r.get("clearedAt"))
                );
                ps.add(cb.greaterThanOrEqualTo(sub.getSelection().as(Integer.class), min));
            }

            // reason exists (활성 신고만 집계 기준)
            if (q.getReason() != null && !q.getReason().isBlank()) {
                var sub = cq.subquery(Long.class);
                var r = sub.from(cotw.server.domain.comment.entity.CommentReport.class);
                sub.select(cb.literal(1L));
                sub.where(
                        cb.equal(r.get("comment").get("id"), root.get("id")),
                        cb.equal(r.get("reason"), ReportReason.valueOf(q.getReason())),
                        cb.isNull(r.get("clearedAt"))
                );
                ps.add(cb.exists(sub));
            }

            // 기간 필터(신고 생성일 기준)
            LocalDateTime from = parseDateTime(q.getFrom());
            LocalDateTime to   = parseDateTime(q.getTo());
            if (from != null) {
                var sub = cq.subquery(Long.class);
                var r = sub.from(cotw.server.domain.comment.entity.CommentReport.class);
                sub.select(cb.literal(1L));
                sub.where(cb.equal(r.get("comment").get("id"), root.get("id")),
                        cb.greaterThanOrEqualTo(r.get("createdAt"), from));
                ps.add(cb.exists(sub));
            }
            if (to != null) {
                var sub = cq.subquery(Long.class);
                var r = sub.from(cotw.server.domain.comment.entity.CommentReport.class);
                sub.select(cb.literal(1L));
                sub.where(cb.equal(r.get("comment").get("id"), root.get("id")),
                        cb.lessThan(r.get("createdAt"), to));
                ps.add(cb.exists(sub));
            }

            // keyword: content / author.email / post.title
            if (q.getKeyword() != null && !q.getKeyword().isBlank()) {
                String like = "%" + q.getKeyword().trim() + "%";
                var post = root.join("post");
                var mem  = root.join("member");
                ps.add(cb.or(
                        cb.like(root.get("content"), like),
                        cb.like(mem.get("email"), like),
                        cb.like(post.get("title"), like)
                ));
            }

            return cb.and(ps.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private LocalDateTime parseDateTime(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            if (s.length() == 10) return LocalDate.parse(s).atStartOfDay();
            return LocalDateTime.parse(s);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private AdminCommentRowResponse toRow(Comment c) {
        // ✅ 활성 신고 수 집계
        int activeCount = (int) reportRepository.countActiveByCommentId(c.getId());

        // ✅ 대표 사유 집계
        String top = null;
        int max = -1;
        for (Object[] row : reportRepository.countActiveByReason(c.getId())) {
            String reason = String.valueOf(row[0]);
            int cnt = ((Number) row[1]).intValue();
            if (cnt > max) { max = cnt; top = reason; }
        }

        return AdminCommentRowResponse.builder()
                .id(c.getId())
                .content(c.getContent())
                .authorEmail(c.getMember().getEmail())
                .postTitle(c.getPost().getTitle())
                .isPublic(c.isPublic())
                .reportCount(activeCount)
                .topReason(top)
                .createdAt(c.getCreatedAt())
                .moderationDueAt(c.getModerationDueAt())
                .build();
    }

    // ===== 단건/벌크 액션 =====
    public void setVisibility(Long id, boolean visible) {
        Comment c = find(id);
        if (visible) { c.makePublic(); } else { c.makePrivate(); }
    }

    public void resetReports(Long id) {
        Comment c = find(id);
        reportRepository.clearByCommentId(id);
        int total = (int) reportRepository.countActiveByCommentId(id);
        c.applyReportTally(total);
        if (total == 0) c.restoreByAdmin();
        // ✅ DB에 즉시 반영
        commentRepository.saveAndFlush(c);
    }

    public void bulkResetReports(List<Long> ids) {
        ids.forEach(this::resetReports);
    }

    public void deleteOne(Long id) {
        Comment c = find(id);
        c.delete(); // soft delete
    }

    public void bulkDelete(List<Long> ids) {
        ids.forEach(this::deleteOne);
    }

    public void bulkVisibility(List<Long> ids, boolean visible) {
        ids.forEach(id -> setVisibility(id, visible));
    }

    // ===== 로그 조회 =====
    @Transactional(readOnly = true)
    public List<AdminReportLogResponse> reportLogs(Long commentId) {
        return reportRepository.findByCommentIdAndClearedAtIsNullOrderByCreatedAtDesc(commentId)
                .stream()
                .map(r -> new AdminReportLogResponse(
                        r.getId(),
                        r.getMember().getEmail(),
                        r.getReason().name(),
                        r.getDetail(),
                        r.getCreatedAt()
                ))
                .toList();
    }

    // ===== util =====
    private Comment find(Long id) {
        return commentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("comment not found: " + id));
    }
}
