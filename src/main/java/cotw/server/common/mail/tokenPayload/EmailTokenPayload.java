package cotw.server.common.mail.tokenPayload;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmailTokenPayload {
    private Long memberId;
    private String email;
    private EmailTokenPurpose purpose;
    private long expiresAtEpochSec;
    private boolean used;
    private String secretHash; // SHA-256(secret)
    private String ip;
    private String ua;
}
