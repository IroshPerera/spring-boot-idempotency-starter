package lk.irosh.idempotency.storage;

import lk.irosh.idempotency.model.IdempotencyRecord;

import java.time.Duration;
import java.util.Optional;

public interface IdempotencyStore {

    Optional<IdempotencyRecord> findByKey(String key);

    boolean acquire(
            String key,
            IdempotencyRecord record,
            Duration ttl
    );

    void saveCompleted(
            String key,
            int httpStatus,
            String responseBody
    );

    void saveFailed(
            String key
    );

    void delete(
            String key
    );
}