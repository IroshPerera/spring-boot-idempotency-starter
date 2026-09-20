package lk.irosh.idempotency.exception;

public class IdempotencyKeyConflictException
        extends RuntimeException {

    public IdempotencyKeyConflictException(String message) {
        super(message);
    }
}