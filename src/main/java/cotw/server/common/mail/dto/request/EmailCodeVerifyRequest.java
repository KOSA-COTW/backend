package cotw.server.common.mail.dto.request;

import cotw.server.common.mail.tokenPayload.EmailTokenPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record EmailCodeVerifyRequest(
        @NotBlank @Email String email,
        @NotNull EmailTokenPurpose purpose,
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "6자리 숫자 코드여야 합니다")
        String code) {
}
