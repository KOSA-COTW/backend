package cotw.server.domain.payment.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "toss")
public class TossPaymentConfig {

    private String clientKey;
    private String secretKey;
    private String apiUrl;
    private String successUrl;
    private String failUrl;
}
