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

public class IdempotencyService {

    private final IdempotencyStore store;

    public IdempotencyService(IdempotencyStore store) {
        this.store = store;
    }

    public Optional<IdempotencyRecord> reserve(
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

            if (record.getStatus()
                    == IdempotencyStatus.PROCESSING) {

                throw new DuplicateRequestException(
                        "The request is already being processed"
                );
            }

            if (record.getStatus()
                    == IdempotencyStatus.COMPLETED) {

                return existing;
            }

            store.delete(key);
        }

        IdempotencyRecord newRecord =
                new IdempotencyRecord(
                        key,
                        requestHash,
                        endpoint,
                        httpMethod,
                        IdempotencyStatus.PROCESSING,
                        Instant.now(),
                        Instant.now().plus(ttl)
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

        return Optional.empty();
    }

    public void complete(
            String key,
            int httpStatus,
            String responseBody
    ) {
        store.saveCompleted(
                key,
                httpStatus,
                responseBody
        );
    }

    public void fail(String key) {
        store.saveFailed(key);
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
}