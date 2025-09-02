package cotw.server.domain.donation;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.RedisScript;

@Configuration
public class DonationScripts {
    private static final String LUA_MULTI_INCR = """
        local amt = tonumber(ARGV[1])
        for _, k in ipairs(KEYS) do
          redis.call('INCRBY', k, amt)
        end
        return 1
    """;
    @Bean
    public RedisScript<Long> multiIncrScript() {
        return RedisScript.of(LUA_MULTI_INCR, Long.class);
    }
}
