package lk.irosh.idempotency.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class IdempotencyExceptionHandler {

    @ExceptionHandler(MissingIdempotencyKeyException.class)
    public ResponseEntity<Map<String, Object>>
    handleMissingKey(
            MissingIdempotencyKeyException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "success", false,
                        "message", exception.getMessage()
                ));
    }

    @ExceptionHandler(IdempotencyKeyConflictException.class)
    public ResponseEntity<Map<String, Object>>
    handleKeyConflict(
            IdempotencyKeyConflictException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "success", false,
                        "message", exception.getMessage()
                ));
    }

    @ExceptionHandler(DuplicateRequestException.class)
    public ResponseEntity<Map<String, Object>>
    handleDuplicateRequest(
            DuplicateRequestException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "success", false,
                        "message", exception.getMessage()
                ));
    }
}