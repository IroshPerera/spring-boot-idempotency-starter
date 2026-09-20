package lk.irosh.idempotency.exception;

public class MissingIdempotencyKeyException
        extends RuntimeException {

    public MissingIdempotencyKeyException(String message) {
        super(message);
    }
}