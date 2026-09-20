package lk.irosh.idempotency.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lk.irosh.idempotency.aspect.IdempotencyAspect;
import lk.irosh.idempotency.exception.IdempotencyExceptionHandler;
import lk.irosh.idempotency.service.IdempotencyService;
import lk.irosh.idempotency.storage.IdempotencyStore;
import lk.irosh.idempotency.storage.InMemoryIdempotencyStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

import jakarta.servlet.http.HttpServletRequest;

@AutoConfiguration
@EnableAspectJAutoProxy
@EnableConfigurationProperties(
        IdempotencyProperties.class
)
@Import(RedisIdempotencyConfiguration.class)
public class IdempotencyAutoConfiguration {

    @Bean
    @ConditionalOnProperty(
            prefix = "idempotency",
            name = "storage",
            havingValue = "memory",
            matchIfMissing = true
    )
    @ConditionalOnMissingBean(IdempotencyStore.class)
    public IdempotencyStore idempotencyStore() {
        return new InMemoryIdempotencyStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public IdempotencyService idempotencyService(
            IdempotencyStore idempotencyStore
    ) {
        return new IdempotencyService(
                idempotencyStore
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public IdempotencyAspect idempotencyAspect(
            IdempotencyService idempotencyService,
            ObjectMapper objectMapper,
            HttpServletRequest request
    ) {
        return new IdempotencyAspect(
                idempotencyService,
                objectMapper,
                request
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public IdempotencyExceptionHandler
    idempotencyExceptionHandler() {
        return new IdempotencyExceptionHandler();
    }
}