package lk.irosh.idempotency.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lk.irosh.idempotency.model.IdempotencyRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.Optional;

public class RedisIdempotencyStore
        implements IdempotencyStore {

    private static final String KEY_PREFIX =
            "idempotency:";

    private final StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper;

    private static final DefaultRedisScript<Long> UPDATE_IF_OWNER =
            new DefaultRedisScript<>(
                    "local current = redis.call('get', KEYS[1]) " +
                            "if not current then return 0 end " +
                            "local owner = '\"reservationId\":\"' .. ARGV[1] .. '\"' " +
                            "if not string.find(current, owner, 1, true) then return 0 end " +
                            "redis.call('set', KEYS[1], ARGV[2], 'EX', ARGV[3]) " +
                            "return 1",
                    Long.class
            );

    private static final DefaultRedisScript<Long> DELETE_IF_OWNER =
            new DefaultRedisScript<>(
                    "local current = redis.call('get', KEYS[1]) " +
                            "if not current then return 0 end " +
                            "local owner = '\"reservationId\":\"' .. ARGV[1] .. '\"' " +
                            "if not string.find(current, owner, 1, true) then return 0 end " +
                            "return redis.call('del', KEYS[1])",
                    Long.class
            );

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
        saveCompleted(key, null, httpStatus, responseBody);
    }

    @Override
    public void saveCompleted(
            String key,
            String reservationId,
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

        if (reservationId != null
                && !reservationId.equals(record.getReservationId())) {
            return;
        }

        record.setStatus(
                lk.irosh.idempotency.model.IdempotencyStatus.COMPLETED
        );

        record.setHttpStatus(httpStatus);
        record.setResponseBody(responseBody);
        record.setCompletedAt(
                java.time.Instant.now()
        );

        saveWithRemainingTtl(key, reservationId, record);
    }

    @Override
    public void saveFailed(String key) {
        saveFailed(key, null);
    }

    @Override
    public void saveFailed(
            String key,
            String reservationId
    ) {
        Optional<IdempotencyRecord> existing =
                findByKey(key);

        if (existing.isEmpty()) {
            return;
        }

        IdempotencyRecord record =
                existing.get();

        if (reservationId != null
                && !reservationId.equals(record.getReservationId())) {
            return;
        }

        record.setStatus(
                lk.irosh.idempotency.model.IdempotencyStatus.FAILED
        );

        record.setCompletedAt(
                java.time.Instant.now()
        );

        saveWithRemainingTtl(key, reservationId, record);
    }

    @Override
    public void delete(String key) {
        redisTemplate.delete(redisKey(key));
    }

    @Override
    public void delete(String key, String reservationId) {
        if (reservationId == null) {
            delete(key);
            return;
        }

        redisTemplate.execute(
                DELETE_IF_OWNER,
                java.util.List.of(redisKey(key)),
                reservationId
        );
    }

    private void saveWithRemainingTtl(
            String key,
            String reservationId,
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

                if (reservationId == null) {
                    redisTemplate.opsForValue().set(
                            redisKey(key),
                            json,
                            Duration.ofSeconds(remainingTtl)
                    );
                } else {
                    redisTemplate.execute(
                            UPDATE_IF_OWNER,
                            java.util.List.of(redisKey(key)),
                            reservationId,
                            json,
                            String.valueOf(remainingTtl)
                    );
                }
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
