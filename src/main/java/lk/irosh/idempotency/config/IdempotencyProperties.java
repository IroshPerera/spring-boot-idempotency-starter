package lk.irosh.idempotency.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "idempotency")
public class IdempotencyProperties {

    private boolean enabled = true;

    private boolean required = true;

    private String headerName = "Idempotency-Key";

    private Duration defaultExpiry = Duration.ofHours(24);

    private Duration processingTimeout = Duration.ofMinutes(5);

    private boolean cacheResponse = true;

    private boolean rejectDifferentRequestHash = true;

    private String storage = "memory";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public Duration getDefaultExpiry() {
        return defaultExpiry;
    }

    public void setDefaultExpiry(Duration defaultExpiry) {
        this.defaultExpiry = defaultExpiry;
    }

    public Duration getProcessingTimeout() {
        return processingTimeout;
    }

    public void setProcessingTimeout(Duration processingTimeout) {
        this.processingTimeout = processingTimeout;
    }

    public boolean isCacheResponse() {
        return cacheResponse;
    }

    public void setCacheResponse(boolean cacheResponse) {
        this.cacheResponse = cacheResponse;
    }

    public boolean isRejectDifferentRequestHash() {
        return rejectDifferentRequestHash;
    }

    public void setRejectDifferentRequestHash(
            boolean rejectDifferentRequestHash
    ) {
        this.rejectDifferentRequestHash =
                rejectDifferentRequestHash;
    }

    public String getStorage() {
        return storage;
    }

    public void setStorage(String storage) {
        this.storage = storage;
    }
}