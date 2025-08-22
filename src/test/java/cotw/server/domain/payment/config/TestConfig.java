package cotw.server.domain.payment.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

@TestConfiguration
public class TestConfig {

    @Bean
    @Primary
    public ObjectMapper testObjectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @Primary
    public RestClient testRestClient() {
        return RestClient.builder().build();
    }


    @Bean
    @Primary
    public TossPaymentConfig testTossPaymentConfig() {
        TossPaymentConfig config = new TossPaymentConfig();
        config.setSecretKey("test_secret_key");
        config.setApiUrl("https://api.tosspayments.com");
        return config;
    }
}