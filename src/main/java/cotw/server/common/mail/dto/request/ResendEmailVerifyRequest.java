package cotw.server.common.mail.dto.request;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ResendEmailVerifyRequest(
        @Email @NotBlank String email
) {}
