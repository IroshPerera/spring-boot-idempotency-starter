# Changelog

## 0.2.0

- Fixed Jackson and SLF4J dependency alignment for Spring Boot tests.
- Added Redis Testcontainers integration coverage.
- Added concurrent Redis key-acquisition coverage.
- Added reservation ownership checks for completion and cleanup.
- Prevented an expired request from overwriting a newer reservation.
- Added stale `PROCESSING` request recovery using `processing-timeout`.
- Made global idempotency properties apply to `@Idempotent` endpoints.
- Added `idempotency.enabled` support.

## 0.1.0

- Initial public release.
