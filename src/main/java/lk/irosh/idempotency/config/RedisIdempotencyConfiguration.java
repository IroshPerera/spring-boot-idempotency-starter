package lk.irosh.idempotency.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lk.irosh.idempotency.storage.IdempotencyStore;
import lk.irosh.idempotency.storage.RedisIdempotencyStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnProperty(
        prefix = "idempotency",
        name = "storage",
        havingValue = "redis"
)
public class RedisIdempotencyConfiguration {

    @Bean
    @ConditionalOnMissingBean(IdempotencyStore.class)
    public IdempotencyStore redisIdempotencyStore(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        return new RedisIdempotencyStore(
                redisTemplate,
                objectMapper
        );
    }
}