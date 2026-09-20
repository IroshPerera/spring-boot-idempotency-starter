package lk.irosh.idempotency.service;

import lk.irosh.idempotency.exception.DuplicateRequestException;
import lk.irosh.idempotency.exception.IdempotencyKeyConflictException;
import lk.irosh.idempotency.exception.MissingIdempotencyKeyException;
import lk.irosh.idempotency.model.IdempotencyRecord;
import lk.irosh.idempotency.model.IdempotencyStatus;
import lk.irosh.idempotency.storage.IdempotencyStore;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class IdempotencyService {

    private final IdempotencyStore store;

    private final Duration processingTimeout;

    public IdempotencyService(IdempotencyStore store) {
        this(store, Duration.ofMinutes(5));
    }

    public IdempotencyService(
            IdempotencyStore store,
            Duration processingTimeout
    ) {
        this.store = store;
        this.processingTimeout = processingTimeout;
    }

    public Optional<IdempotencyRecord> reserve(
            String key,
            String requestHash,
            String endpoint,
            String httpMethod,
            Duration ttl
    ) {
        return reserveRequest(
                key,
                requestHash,
                endpoint,
                httpMethod,
                ttl
        ).completedRecord();
    }

    public IdempotencyReservation reserveRequest(
            String key,
            String requestHash,
            String endpoint,
            String httpMethod,
            Duration ttl
    ) {
        validateKey(key);

        Optional<IdempotencyRecord> existing =
                store.findByKey(key);

        if (existing.isPresent()) {
            IdempotencyRecord record = existing.get();

            validateSameRequest(
                    record,
                    requestHash
            );

            if (record.getStatus() == IdempotencyStatus.PROCESSING) {
                if (!isProcessingExpired(record)) {
                    throw new DuplicateRequestException(
                            "The request is already being processed"
                    );
                }

                store.delete(key);
            }

            if (record.getStatus() == IdempotencyStatus.COMPLETED) {

                return new IdempotencyReservation(
                        existing,
                        null
                );
            }

            if (record.getStatus() == IdempotencyStatus.FAILED) {
                store.delete(key);
            }
        }

        Instant createdAt = Instant.now();
        IdempotencyRecord newRecord =
                new IdempotencyRecord(
                        key,
                        UUID.randomUUID().toString(),
                        requestHash,
                        endpoint,
                        httpMethod,
                        IdempotencyStatus.PROCESSING,
                        createdAt,
                        createdAt.plus(ttl)
                );

        boolean acquired = store.acquire(
                key,
                newRecord,
                ttl
        );

        if (!acquired) {
            throw new DuplicateRequestException(
                    "The request is already being processed"
            );
        }

        return new IdempotencyReservation(
                Optional.empty(),
                newRecord.getReservationId()
        );
    }

    public void complete(
            String key,
            int httpStatus,
            String responseBody
    ) {
        Optional<IdempotencyRecord> existing = store.findByKey(key);

        existing.ifPresent(record -> complete(
                key,
                record.getReservationId(),
                httpStatus,
                responseBody
        ));
    }

    public void complete(
            String key,
            String reservationId,
            int httpStatus,
            String responseBody
    ) {
        store.saveCompleted(
                key,
                reservationId,
                httpStatus,
                responseBody
        );
    }

    public void fail(String key) {
        Optional<IdempotencyRecord> existing = store.findByKey(key);

        existing.ifPresent(record -> fail(
                key,
                record.getReservationId()
        ));
    }

    public void fail(
            String key,
            String reservationId
    ) {
        store.saveFailed(key, reservationId);
    }

    public void release(
            String key,
            String reservationId
    ) {
        store.delete(key, reservationId);
    }

    private void validateKey(String key) {

        if (key == null || key.isBlank()) {
            throw new MissingIdempotencyKeyException(
                    "Idempotency-Key header is required"
            );
        }
    }

    private void validateSameRequest(
            IdempotencyRecord record,
            String requestHash
    ) {
        if (!record.getRequestHash()
                .equals(requestHash)) {

            throw new IdempotencyKeyConflictException(
                    "This idempotency key was already used " +
                            "with a different request"
            );
        }
    }

    private boolean isProcessingExpired(IdempotencyRecord record) {
        Instant createdAt = record.getCreatedAt();

        return createdAt != null
                && createdAt.plus(processingTimeout)
                .isBefore(Instant.now());
    }
}
