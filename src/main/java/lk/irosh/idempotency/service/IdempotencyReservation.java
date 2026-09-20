package lk.irosh.idempotency.service;

import lk.irosh.idempotency.model.IdempotencyRecord;

import java.util.Optional;

/**
 * Result of reserving an idempotency key.
 *
 * <p>A completed record is returned as a replay. For a newly acquired key,
 * the reservation id is used to ensure that only the request which acquired
 * the key can complete or fail it.</p>
 */
public record IdempotencyReservation(
        Optional<IdempotencyRecord> completedRecord,
        String reservationId
) {

    public boolean isReplay() {
        return completedRecord.isPresent();
    }
}
