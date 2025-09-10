package cotw.server.common.mail.dto.request;

import cotw.server.common.mail.tokenPayload.EmailTokenPurpose;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailCodeSendRequest(
        @NotBlank @Email String email,
        EmailTokenPurpose purpose
) {}
