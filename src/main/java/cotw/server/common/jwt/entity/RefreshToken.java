package cotw.server.common.jwt.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.redis.core.RedisHash;

import java.util.Date;

@Entity
@Setter
@Getter
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;

    /** SHA-256 토큰 해시 (원문 저장 금지) */
    @Column(length = 64, unique = true, nullable = false)
    private String tokenHash;

    private String expiryDate;

}
