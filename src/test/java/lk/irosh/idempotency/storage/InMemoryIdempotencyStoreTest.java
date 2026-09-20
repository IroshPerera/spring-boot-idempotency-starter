package lk.irosh.idempotency.storage;

import lk.irosh.idempotency.model.IdempotencyRecord;
import lk.irosh.idempotency.model.IdempotencyStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryIdempotencyStoreTest {

    private InMemoryIdempotencyStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryIdempotencyStore();
    }

    @Test
    void shouldAcquireNewKey() {

        IdempotencyRecord record =
                new IdempotencyRecord(
                        "key-123",
                        "hash-123",
                        "/orders",
                        "POST",
                        IdempotencyStatus.PROCESSING,
                        Instant.now(),
                        Instant.now().plusSeconds(60)
                );

        boolean acquired = store.acquire(
                "key-123",
                record,
                Duration.ofMinutes(5)
        );

        assertTrue(acquired);
        assertTrue(
                store.findByKey("key-123").isPresent()
        );
    }

    @Test
    void shouldNotAcquireSameKeyTwice() {

        IdempotencyRecord firstRecord =
                createRecord();

        IdempotencyRecord secondRecord =
                createRecord();

        boolean firstResult = store.acquire(
                "same-key",
                firstRecord,
                Duration.ofMinutes(5)
        );

        boolean secondResult = store.acquire(
                "same-key",
                secondRecord,
                Duration.ofMinutes(5)
        );

        assertTrue(firstResult);
        assertFalse(secondResult);
    }

    @Test
    void shouldSaveCompletedResponse() {

        IdempotencyRecord record =
                createRecord();

        store.acquire(
                "completed-key",
                record,
                Duration.ofMinutes(5)
        );

        store.saveCompleted(
                "completed-key",
                201,
                "{\"id\":1}"
        );

        Optional<IdempotencyRecord> result =
                store.findByKey("completed-key");

        assertTrue(result.isPresent());
        assertEquals(
                IdempotencyStatus.COMPLETED,
                result.get().getStatus()
        );
        assertEquals(
                201,
                result.get().getHttpStatus()
        );
        assertEquals(
                "{\"id\":1}",
                result.get().getResponseBody()
        );
    }

    @Test
    void shouldDeleteKey() {

        IdempotencyRecord record =
                createRecord();

        store.acquire(
                "delete-key",
                record,
                Duration.ofMinutes(5)
        );

        store.delete("delete-key");

        assertTrue(
                store.findByKey("delete-key").isEmpty()
        );
    }

    private IdempotencyRecord createRecord() {
        return new IdempotencyRecord(
                "test-key",
                "test-hash",
                "/test",
                "POST",
                IdempotencyStatus.PROCESSING,
                Instant.now(),
                Instant.now().plusSeconds(60)
        );
    }
}