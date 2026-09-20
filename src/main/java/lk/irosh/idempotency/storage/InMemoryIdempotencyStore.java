package lk.irosh.idempotency.storage;

import lk.irosh.idempotency.model.IdempotencyRecord;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryIdempotencyStore
        implements IdempotencyStore {

    private final ConcurrentMap<String, IdempotencyRecord> records =
            new ConcurrentHashMap<>();

    @Override
    public Optional<IdempotencyRecord> findByKey(String key) {

        IdempotencyRecord record = records.get(key);

        if (record == null) {
            return Optional.empty();
        }

        if (isExpired(record)) {
            records.remove(key);
            return Optional.empty();
        }

        return Optional.of(record);
    }

    @Override
    public boolean acquire(
            String key,
            IdempotencyRecord record,
            Duration ttl
    ) {
        record.setExpiresAt(
                Instant.now().plus(ttl)
        );

        IdempotencyRecord existing =
                records.putIfAbsent(key, record);

        return existing == null;
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
        records.computeIfPresent(key, (ignored, record) -> {
            if (!matchesReservation(record, reservationId)) {
                return record;
            }

            record.setStatus(
                    lk.irosh.idempotency.model.IdempotencyStatus.COMPLETED
            );
            record.setHttpStatus(httpStatus);
            record.setResponseBody(responseBody);
            record.setCompletedAt(Instant.now());
            return record;
        });
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
        records.computeIfPresent(key, (ignored, record) -> {
            if (!matchesReservation(record, reservationId)) {
                return record;
            }

            record.setStatus(
                    lk.irosh.idempotency.model.IdempotencyStatus.FAILED
            );
            record.setCompletedAt(Instant.now());
            return record;
        });
    }

    @Override
    public void delete(String key) {
        records.remove(key);
    }

    @Override
    public void delete(String key, String reservationId) {
        records.computeIfPresent(key, (ignored, record) ->
                matchesReservation(record, reservationId)
                        ? null
                        : record
        );
    }

    private boolean isExpired(IdempotencyRecord record) {

        return record.getExpiresAt() != null
                && record.getExpiresAt().isBefore(Instant.now());
    }

    private boolean matchesReservation(
            IdempotencyRecord record,
            String reservationId
    ) {
        return reservationId == null
                || reservationId.equals(record.getReservationId());
    }
}
