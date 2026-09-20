package lk.irosh.idempotency.exception;

public class DuplicateRequestException
        extends RuntimeException {

    public DuplicateRequestException(String message) {
        super(message);
    }
}