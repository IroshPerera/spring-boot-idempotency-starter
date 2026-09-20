package lk.irosh.idempotency.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import lk.irosh.idempotency.model.IdempotencyRecord;
import lk.irosh.idempotency.model.IdempotencyStatus;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class RedisIdempotencyStoreTest {

    @Container
    static final GenericContainer<?> redis =
            new GenericContainer<>(
                    DockerImageName.parse("redis:7-alpine")
            )
                    .withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;

    private static RedisIdempotencyStore store;

    @BeforeAll
    static void setUp() {
        RedisStandaloneConfiguration configuration =
                new RedisStandaloneConfiguration(
                        redis.getHost(),
                        redis.getMappedPort(6379)
                );

        connectionFactory = new LettuceConnectionFactory(configuration);
        connectionFactory.afterPropertiesSet();

        StringRedisTemplate redisTemplate =
                new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();

        store = new RedisIdempotencyStore(
                redisTemplate,
                new ObjectMapper().findAndRegisterModules()
        );
    }

    @AfterAll
    static void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void shouldAcquireAKeyOnlyOnceAcrossConcurrentRequests()
            throws Exception {

        String key = "redis-concurrent-" + System.nanoTime();
        ExecutorService executor = Executors.newFixedThreadPool(8);

        try {
            List<Callable<Boolean>> attempts = new ArrayList<>();

            for (int i = 0; i < 8; i++) {
                String reservationId = "reservation-" + i;
                attempts.add(() -> store.acquire(
                        key,
                        createRecord(key, reservationId),
                        Duration.ofMinutes(1)
                ));
            }

            List<Future<Boolean>> results = executor.invokeAll(attempts);
            long acquired = 0;

            for (Future<Boolean> result : results) {
                if (result.get()) {
                    acquired++;
                }
            }

            assertEquals(1, acquired);
        } finally {
            executor.shutdownNow();
            store.delete(key);
        }
    }

    @Test
    void shouldSaveCompletedResponseAndKeepTheRecordTtl() {

        String key = "redis-completed-" + System.nanoTime();
        IdempotencyRecord record = createRecord(key, "owner-1");

        try {
            assertTrue(store.acquire(key, record, Duration.ofSeconds(30)));

            store.saveCompleted(
                    key,
                    "owner-1",
                    201,
                    "{\"id\":1}"
            );

            Optional<IdempotencyRecord> result = store.findByKey(key);

            assertTrue(result.isPresent());
            assertEquals(
                    IdempotencyStatus.COMPLETED,
                    result.get().getStatus()
            );
            assertEquals(201, result.get().getHttpStatus());
            assertEquals("{\"id\":1}", result.get().getResponseBody());
        } finally {
            store.delete(key);
        }
    }

    @Test
    void shouldNotAllowAnOldReservationToOverwriteTheCurrentRecord() {

        String key = "redis-owner-" + System.nanoTime();
        IdempotencyRecord oldRecord = createRecord(key, "old-owner");
        IdempotencyRecord currentRecord = createRecord(key, "current-owner");

        try {
            assertTrue(store.acquire(key, oldRecord, Duration.ofMinutes(1)));
            store.delete(key);
            assertTrue(store.acquire(key, currentRecord, Duration.ofMinutes(1)));

            store.saveCompleted(
                    key,
                    "old-owner",
                    200,
                    "{\"stale\":true}"
            );

            Optional<IdempotencyRecord> result = store.findByKey(key);

            assertTrue(result.isPresent());
            assertFalse(result.get().getStatus()
                    == IdempotencyStatus.COMPLETED);
            assertEquals("current-owner", result.get().getReservationId());
        } finally {
            store.delete(key);
        }
    }

    private static IdempotencyRecord createRecord(
            String key,
            String reservationId
    ) {
        Instant now = Instant.now();

        return new IdempotencyRecord(
                key,
                reservationId,
                "request-hash",
                "/orders",
                "POST",
                IdempotencyStatus.PROCESSING,
                now,
                now.plusSeconds(60)
        );
    }
}
