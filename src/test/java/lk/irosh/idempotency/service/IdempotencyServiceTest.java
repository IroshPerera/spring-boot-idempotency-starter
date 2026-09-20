package lk.irosh.idempotency.service;

import lk.irosh.idempotency.exception.DuplicateRequestException;
import lk.irosh.idempotency.exception.IdempotencyKeyConflictException;
import lk.irosh.idempotency.model.IdempotencyRecord;
import lk.irosh.idempotency.storage.InMemoryIdempotencyStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class IdempotencyServiceTest {

    private IdempotencyService service;

    @BeforeEach
    void setUp() {
        service = new IdempotencyService(
                new InMemoryIdempotencyStore()
        );
    }

    @Test
    void shouldAcceptNewRequest() {

        Optional<IdempotencyRecord> result =
                service.reserve(
                        "key-1",
                        "hash-1",
                        "/orders",
                        "POST",
                        Duration.ofMinutes(5)
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldRejectRequestWhileProcessing() {

        service.reserve(
                "key-2",
                "hash-2",
                "/orders",
                "POST",
                Duration.ofMinutes(5)
        );

        assertThrows(
                DuplicateRequestException.class,
                () -> service.reserve(
                        "key-2",
                        "hash-2",
                        "/orders",
                        "POST",
                        Duration.ofMinutes(5)
                )
        );
    }

    @Test
    void shouldReturnCompletedRequest() {

        service.reserve(
                "key-3",
                "hash-3",
                "/orders",
                "POST",
                Duration.ofMinutes(5)
        );

        service.complete(
                "key-3",
                201,
                "{\"id\":100}"
        );

        Optional<IdempotencyRecord> result =
                service.reserve(
                        "key-3",
                        "hash-3",
                        "/orders",
                        "POST",
                        Duration.ofMinutes(5)
                );

        assertTrue(result.isPresent());
        assertEquals(
                201,
                result.get().getHttpStatus()
        );
        assertEquals(
                "{\"id\":100}",
                result.get().getResponseBody()
        );
    }

    @Test
    void shouldRejectDifferentRequestHash() {

        service.reserve(
                "key-4",
                "hash-original",
                "/orders",
                "POST",
                Duration.ofMinutes(5)
        );

        assertThrows(
                IdempotencyKeyConflictException.class,
                () -> service.reserve(
                        "key-4",
                        "hash-different",
                        "/orders",
                        "POST",
                        Duration.ofMinutes(5)
                )
        );
    }

    @Test
    void shouldReclaimAnExpiredProcessingRequest() throws InterruptedException {

        service = new IdempotencyService(
                new InMemoryIdempotencyStore(),
                Duration.ofMillis(20)
        );

        service.reserve(
                "expired-processing-key",
                "hash-1",
                "/orders",
                "POST",
                Duration.ofMinutes(5)
        );

        Thread.sleep(40);

        Optional<IdempotencyRecord> result = service.reserve(
                "expired-processing-key",
                "hash-1",
                "/orders",
                "POST",
                Duration.ofMinutes(5)
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldExposeAReservationIdForTheRequestOwner() {

        IdempotencyReservation reservation = service.reserveRequest(
                "owner-key",
                "hash-owner",
                "/orders",
                "POST",
                Duration.ofMinutes(5)
        );

        assertFalse(reservation.isReplay());
        assertNotNull(reservation.reservationId());
    }
}
