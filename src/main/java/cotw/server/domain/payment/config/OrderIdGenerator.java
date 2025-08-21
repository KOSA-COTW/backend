package cotw.server.domain.payment.config;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class OrderIdGenerator {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom random = new SecureRandom();

    public String generateOrderId() {
        // 현재 시간을 기반으로 한 접두사 (YYYYMMDDHHMMSS)
        String timePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        // 6자리 랜덤 문자열
        StringBuilder randomSuffix = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            randomSuffix.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }

        return "ORDER_" + timePrefix + "_" + randomSuffix.toString();
    }
}
