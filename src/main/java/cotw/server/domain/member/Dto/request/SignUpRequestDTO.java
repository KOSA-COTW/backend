package cotw.server.domain.member.Dto.request;

import cotw.server.domain.member.entity.Member;
import cotw.server.domain.member.entity.Role;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

public record SignUpRequestDTO(
        String name,

        @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "이메일 형식이 유효하지 않습니다")
        @NotNull(message = "email은 필수 값 입니다")
        String email,

        @NotNull(message = "password는 필수 값 입니다")
        String password,

        String nickname
) {

    public Member toEntity(String name,String nickname, String email, String password,String pictureUrl, Role role) {
        return Member.builder()
                .name(name)
                .nickname(nickname)
                .email(email)
                .password(password)
                .pictureUrl(pictureUrl)
                .role(role)
                .build();
    }
}
