package cotw.server.domain.board.entity;

import lombok.Getter;

@Getter
public enum Category {
    CHILD("아동"),
    DISABLED("장애인"),
    SENIOR("어르신"),
    ANIMAL("동물"),
    ENVIRONMENT("환경"),
    GLOBAL("지구촌"),
    SOCIETY("사회");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }
}
