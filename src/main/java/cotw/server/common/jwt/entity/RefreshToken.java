package cotw.server.common.jwt.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Entity
@Setter
@Getter
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    
    @Column(length = 512)  // JWT 토큰 길이를 고려하여 512자로 확장
    private String refreshToken;

    private String expiryDate;

}
