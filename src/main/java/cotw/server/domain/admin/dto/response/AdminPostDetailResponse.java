package cotw.server.domain.admin.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;


public record AdminPostDetailResponse(
        Long postId,
        String title,
        String content,
        String status,          // Post.getStatus()
        boolean isPublic,
        int amount,             // 목표 금액 (Post.amount)
        int currentAmount,      // 현재 모금 금액 (Post.currentAmount)
        LocalDate deadline,     // 마감일
        int donorCount,         // 참여자 수 (participants.size)
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
