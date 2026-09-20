# Spring Boot Idempotency Starter

[![Maven Central](https://img.shields.io/maven-central/v/io.github.iroshperera/spring-boot-idempotency-starter.svg)](https://central.sonatype.com/artifact/io.github.iroshperera/spring-boot-idempotency-starter)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Maven Build](https://github.com/IroshPerera/spring-boot-idempotency-starter/actions/workflows/maven.yml/badge.svg)](https://github.com/IroshPerera/spring-boot-idempotency-starter/actions/workflows/maven.yml)

A reusable Spring Boot starter that prevents duplicate execution of important API requests such as orders, payments, bookings, and other operations that must be processed only once.

## Features

- Prevents duplicate request execution
- Supports the `Idempotency-Key` request header
- Validates request body hashes
- Caches completed responses
- In-memory storage
- Redis storage
- Configurable record expiry
- Configurable processing timeout
- Automatic Spring Boot auto-configuration
- Handles missing keys and key conflicts
- Atomically reserves Redis keys before controller execution
- Prevents an expired request from overwriting a newer reservation
- Reclaims stale `PROCESSING` records after the configured timeout
- Compatible with Maven Central

## Requirements

- Java 17 or higher
- Spring Boot 3.x
- Maven 3.8 or higher

## Installation

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.iroshperera</groupId>
    <artifactId>spring-boot-idempotency-starter</artifactId>
    <version>0.2.0</version>
</dependency>
```

## Basic Usage

Add the `@Idempotent` annotation to an important `POST` endpoint:

```java
import lk.irosh.idempotency.annotation.Idempotent;

@Idempotent
@PostMapping("/orders")
public OrderResponse createOrder(
        @RequestBody OrderRequest request
) {
    return orderService.create(request);
}
```

## Request Header

Every protected request must include a unique idempotency key:

```http
Idempotency-Key: order-request-001
```

The same key should be used when retrying the same request.

## Configuration

Create `application.yml` in your application:

```yaml
idempotency:
  enabled: true
  storage: memory
  required: true
  header-name: Idempotency-Key
  default-expiry: 24h
  processing-timeout: 5m
  cache-response: true
  reject-different-request-hash: true
```

The global configuration is used by `@Idempotent` unless a method-level
annotation value overrides it. Set `idempotency.enabled` to `false` to disable
the starter without removing the dependency.

### Configuration Properties

| Property | Description | Example |
|---|---|---|
| `idempotency.enabled` | Enables or disables idempotency processing | `true` |
| `idempotency.storage` | Storage implementation | `memory` or `redis` |
| `idempotency.required` | Requires the idempotency header | `true` |
| `idempotency.header-name` | Request header name | `Idempotency-Key` |
| `idempotency.default-expiry` | Stored record expiry duration | `24h` |
| `idempotency.processing-timeout` | Maximum processing duration | `5m` |
| `idempotency.cache-response` | Caches completed responses | `true` |
| `idempotency.reject-different-request-hash` | Rejects the same key with a different body | `true` |

## Redis Configuration

Set the storage mode to Redis:

```yaml
idempotency:
  storage: redis

spring:
  data:
    redis:
      host: localhost
      port: 6379
```

Make sure a Redis server is running before starting the application.

## Request Behaviour

### First request

The controller is executed and the response is saved using the idempotency key.

### Repeated request

The saved response is returned without executing the controller again.

For Redis storage, the first request atomically reserves the key using Redis
`SET NX` semantics. A concurrent request with the same key is rejected while
the first request is still processing. After completion, the stored response
is returned on retries.

### Same key with a different body

The request is rejected with HTTP `409 Conflict`.

## Error Responses

### Missing Idempotency Key

HTTP status: `400 Bad Request`

```json
{
  "success": false,
  "message": "Idempotency-Key header is required"
}
```

### Different Request Body

HTTP status: `409 Conflict`

```json
{
  "success": false,
  "message": "This idempotency key was already used with a different request"
}
```

## Storage Options

### In-memory storage

Suitable for local development, testing, and single-instance applications.

### Redis storage

Recommended for distributed or multi-instance applications where all application instances must share idempotency records.

Redis completion and cleanup operations are protected by a reservation id, so
an older request cannot overwrite or delete a newer reservation after a key
expires and is reused.

## Example cURL Request

```bash
curl -X POST http://localhost:8080/api/orders \\
  -H "Content-Type: application/json" \\
  -H "Idempotency-Key: order-request-001" \\
  -d '{"item":"Laptop","quantity":1}'
```

Repeat the same request with the same key to receive the cached response.

## Building From Source

```bash
mvn clean test
```

To build release artifacts with signatures:

```bash
mvn clean verify -Prelease
```

The Redis integration tests use Testcontainers. They run automatically when a
Docker environment is available and are skipped when Docker is unavailable.

## Maven Coordinates

```text
Group ID:    io.github.iroshperera
Artifact ID: spring-boot-idempotency-starter
Version:     0.2.0
```

## Source Code

GitHub repository:

https://github.com/IroshPerera/spring-boot-idempotency-starter

## Version 0.2.0

- Redis atomic acquisition coverage
- Concurrent Redis acquisition test
- Reservation ownership checks for completion and deletion
- Stale processing timeout handling
- Jackson and logging dependency alignment

## Roadmap

- JDBC and PostgreSQL storage
- MongoDB storage
- Distributed locking improvements
- Metrics and monitoring support
- Additional integration tests

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
