package lk.irosh.idempotency.model;

import java.time.Instant;

public class IdempotencyRecord {

    private String key;

    private String requestHash;

    private String endpoint;

    private String httpMethod;

    private IdempotencyStatus status;

    private Integer httpStatus;

    private String responseBody;

    private Instant createdAt;

    private Instant completedAt;

    private Instant expiresAt;

    public IdempotencyRecord() {
    }

    public IdempotencyRecord(
            String key,
            String requestHash,
            String endpoint,
            String httpMethod,
            IdempotencyStatus status,
            Instant createdAt,
            Instant expiresAt
    ) {
        this.key = key;
        this.requestHash = requestHash;
        this.endpoint = endpoint;
        this.httpMethod = httpMethod;
        this.status = status;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public void setRequestHash(String requestHash) {
        this.requestHash = requestHash;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public IdempotencyStatus getStatus() {
        return status;
    }

    public void setStatus(IdempotencyStatus status) {
        this.status = status;
    }

    public Integer getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(Integer httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}