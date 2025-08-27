package cotw.server.common.jwt.service;

import java.time.Duration;
public interface RefreshTokenService {
    void save(String email, String refreshToken, Duration ttl);
    boolean exists(String refreshToken);
    void revoke(String refreshToken);
    void revokeAllByUser(String email);
}

