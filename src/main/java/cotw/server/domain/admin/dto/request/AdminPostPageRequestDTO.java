package cotw.server.domain.admin.dto.request;

import cotw.server.domain.board.entity.Category;
import cotw.server.domain.board.entity.PostVisibility;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminPostPageRequestDTO {
    
    private int page = 1;
    private int limit = 10;
    
    private PostVisibility visibility;
    private Category category;
    private String title;
    private String authorName;
    
    private String sortBy = "date";
    private String sortDirection = "desc";
    
    public void validateAndSetDefaults() {
        if (limit != 10 && limit != 20 && limit != 50) {
            limit = 10;
        }
        
        if (page < 1) {
            page = 1;
        }
        
        if (sortBy == null || (!sortBy.equals("date") && !sortBy.equals("title"))) {
            sortBy = "date";
        }
        
        if (sortDirection == null || (!sortDirection.equals("asc") && !sortDirection.equals("desc"))) {
            sortDirection = "desc";
        }
    }
}