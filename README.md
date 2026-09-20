# Spring Boot Idempotency Starter

A reusable Spring Boot starter that prevents duplicate execution of important API requests such as orders, payments and bookings.

## Features

- Prevents duplicate requests
- Supports `Idempotency-Key` header
- Request body hash validation
- Response caching
- In-memory storage
- Redis storage
- Configurable expiry time
- Automatic Spring Boot configuration
- Duplicate request exception handling

## Installation

```xml
<dependency>
    <groupId>io.github.iroshperera</groupId>
    <artifactId>spring-boot-idempotency-starter</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Basic Usage

```java
@Idempotent
@PostMapping("/orders")
public OrderResponse createOrder(
        @RequestBody OrderRequest request
) {
    return orderService.create(request);
}
```

## Request Header

```http
Idempotency-Key: order-request-001
```

The same key should be used when retrying the same request.

## Configuration

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

## Redis Configuration

```yaml
idempotency:
  storage: redis

spring:
  data:
    redis:
      host: localhost
      port: 6379
```

## Request Behaviour

```text
First request:
The controller is executed and the response is saved.

Repeated request:
The saved response is returned without executing the controller again.

Same key with different body:
The request is rejected with HTTP 409 Conflict.
```

## Error Responses

### Missing Idempotency Key

```json
{
  "success": false,
  "message": "Idempotency-Key header is required"
}
```

### Different Request Body

```json
{
  "success": false,
  "message": "This idempotency key was already used with a different request"
}
```

## Build

```bash
mvn clean test
```

## License

MIT License