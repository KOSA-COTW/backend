package cotw.server.domain.admin.dto.response;

import cotw.server.domain.board.entity.Category;

public record AdminCategoryStatsResponse(
        Category category,
        String categoryName,
        long totalAmount
) {}