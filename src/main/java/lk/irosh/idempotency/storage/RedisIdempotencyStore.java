package lk.irosh.idempotency.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lk.irosh.idempotency.model.IdempotencyRecord;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.Optional;

public class RedisIdempotencyStore
        implements IdempotencyStore {

    private static final String KEY_PREFIX =
            "idempotency:";

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    public RedisIdempotencyStore(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public Optional<IdempotencyRecord> findByKey(
            String key
    ) {
        String json = redisTemplate
                .opsForValue()
                .get(redisKey(key));

        if (json == null) {
            return Optional.empty();
        }

        try {
            return Optional.of(
                    objectMapper.readValue(
                            json,
                            IdempotencyRecord.class
                    )
            );

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Unable to read idempotency record",
                    exception
            );
        }
    }

    @Override
    public boolean acquire(
            String key,
            IdempotencyRecord record,
            Duration ttl
    ) {
        try {
            String json =
                    objectMapper.writeValueAsString(record);

            Boolean stored = redisTemplate
                    .opsForValue()
                    .setIfAbsent(
                            redisKey(key),
                            json,
                            ttl
                    );

            return Boolean.TRUE.equals(stored);

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Unable to save idempotency record",
                    exception
            );
        }
    }

    @Override
    public void saveCompleted(
            String key,
            int httpStatus,
            String responseBody
    ) {
        Optional<IdempotencyRecord> existing =
                findByKey(key);

        if (existing.isEmpty()) {
            return;
        }

        IdempotencyRecord record =
                existing.get();

        record.setStatus(
                lk.irosh.idempotency.model.IdempotencyStatus.COMPLETED
        );

        record.setHttpStatus(httpStatus);
        record.setResponseBody(responseBody);
        record.setCompletedAt(
                java.time.Instant.now()
        );

        saveWithRemainingTtl(key, record);
    }

    @Override
    public void saveFailed(String key) {

        Optional<IdempotencyRecord> existing =
                findByKey(key);

        if (existing.isEmpty()) {
            return;
        }

        IdempotencyRecord record =
                existing.get();

        record.setStatus(
                lk.irosh.idempotency.model.IdempotencyStatus.FAILED
        );

        record.setCompletedAt(
                java.time.Instant.now()
        );

        saveWithRemainingTtl(key, record);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(redisKey(key));
    }

    private void saveWithRemainingTtl(
            String key,
            IdempotencyRecord record
    ) {
        try {
            String json =
                    objectMapper.writeValueAsString(record);

            Long remainingTtl =
                    redisTemplate.getExpire(
                            redisKey(key)
                    );

            if (remainingTtl != null
                    && remainingTtl > 0) {

                redisTemplate.opsForValue().set(
                        redisKey(key),
                        json,
                        Duration.ofSeconds(
                                remainingTtl
                        )
                );
            }

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Unable to update idempotency record",
                    exception
            );
        }
    }

    private String redisKey(String key) {
        return KEY_PREFIX + key;
    }
}