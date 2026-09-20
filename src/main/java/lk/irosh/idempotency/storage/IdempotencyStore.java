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

    /**
     * Completes a reservation only when the reservation id matches.
     * Implementations should override this method when they support owner
     * checks. The default keeps compatibility with existing custom stores.
     */
    default void saveCompleted(
            String key,
            String reservationId,
            int httpStatus,
            String responseBody
    ) {
        saveCompleted(key, httpStatus, responseBody);
    }

    void saveFailed(
            String key
    );

    /**
     * Marks a reservation as failed only when the reservation id matches.
     */
    default void saveFailed(
            String key,
            String reservationId
    ) {
        saveFailed(key);
    }

    void delete(
            String key
    );

    /**
     * Deletes a reservation only when the reservation id matches.
     */
    default void delete(
            String key,
            String reservationId
    ) {
        delete(key);
    }
}
