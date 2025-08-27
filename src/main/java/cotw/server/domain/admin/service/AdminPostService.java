package cotw.server.domain.admin.service;

import cotw.server.domain.admin.dto.request.AdminPostStatusUpdateRequest;
import cotw.server.domain.admin.dto.response.AdminPostDetailResponse;
import cotw.server.domain.admin.dto.response.AdminPostSummaryResponse;
import cotw.server.domain.board.entity.Post;
import cotw.server.domain.board.repository.PostRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminPostService {

    private final PostRepository postRepository;

    @Transactional(readOnly = true)
    public Page<AdminPostSummaryResponse> list(Pageable pageable) {
        return postRepository.findAll(pageable).map(this::toSummary);
    }

    @Transactional(readOnly = true)
    public AdminPostDetailResponse detail(Long postId) {
        Post p = get(postId);
        return toDetail(p);
    }

    /**
     * 상태/공개여부/마감일 변경
     * - status: "COMPLETED" 이면 보수적으로 마감일을 오늘-1로 당겨 종료 처리
     *           "ONGOING"  이면 이미 마감된 경우 지금+30일로 기본 연장(프론트에서 deadline 보내면 그대로 반영)
     * - isPublic: changeVisibility() 사용
     * - deadline: 명시되면 우선 반영
     */
    public void updateStatus(AdminPostStatusUpdateRequest req) {
        Post p = get(req.postId());

        // 공개여부
        if (req.isPublic() != null) {
            p.changeVisibility(req.isPublic());
        }

        // 마감일 명시 시 우선 반영
        if (req.deadline() != null) {
            // 유효성: 과거 날짜 허용(완료 처리 용도) / 필요 시 검증 로직 추가
            setDeadline(p, req.deadline());
        }

        // 상태 문자열 처리 (엔티티에는 setStatus가 없고, getStatus()는 계산값이므로 deadline을 조정)
        if (req.status() != null) {
            String status = req.status().toUpperCase();
            if ("COMPLETED".equals(status)) {
                // 완료 처리: 마감일을 오늘-1로
                setDeadline(p, LocalDate.now().minusDays(1));
            } else if ("ONGOING".equals(status)) {
                // 진행 처리: 이미 완료 상태라면 기본 30일 연장 (프론트에서 deadline 주면 위에서 반영됨)
                if ("COMPLETED".equals(p.getStatus())) {
                    LocalDate base = LocalDate.now().plusDays(30);
                    setDeadline(p, base);
                }
            }
        }
    }

    /** 공개/비공개 토글 */
    public void toggleVisibility(Long postId) {
        Post p = get(postId);
        p.changeVisibility(!p.isPublic());
    }

    private Post get(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("기부글을 찾을 수 없습니다: " + id));
    }

    private void setDeadline(Post p, LocalDate date) {
        // Post.update(dto)만 있는 상태라 직접 필드 접근이 어려우면 setter 추가가 필요.
        // 여기선 단순화를 위해 리플렉션 대신 엔티티에 setter가 있다고 가정.
        // 만약 setter가 없다면 Post에 setDeadline(LocalDate d) 하나 추가하세요.
        try {
            var f = Post.class.getDeclaredField("deadline");
            f.setAccessible(true);
            f.set(p, date);
        } catch (Exception e) {
            throw new IllegalStateException("Post.deadline 필드에 접근할 수 없습니다. setDeadline 추가를 권장합니다.", e);
        }
    }

    private AdminPostSummaryResponse toSummary(Post p) {
        return new AdminPostSummaryResponse(
                p.getId(),
                p.getTitle(),
                p.getStatus(), // "ONGOING" / "COMPLETED"
                p.isPublic(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    private AdminPostDetailResponse toDetail(Post p) {
        return new AdminPostDetailResponse(
                p.getId(),
                p.getTitle(),
                p.getContent(),
                p.getStatus(),
                p.isPublic(),
                p.getAmount(),
                p.getCurrentAmount(),
                p.getDeadline(),
                p.getDonorCount(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
