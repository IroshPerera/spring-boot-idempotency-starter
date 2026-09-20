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
    <version>0.1.0</version>
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

## Maven Coordinates

```text
Group ID:    io.github.iroshperera
Artifact ID: spring-boot-idempotency-starter
Version:     0.1.0
```

## Source Code

GitHub repository:

https://github.com/IroshPerera/spring-boot-idempotency-starter

## Roadmap

- JDBC and PostgreSQL storage
- MongoDB storage
- Distributed locking improvements
- Metrics and monitoring support
- Additional integration tests

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.