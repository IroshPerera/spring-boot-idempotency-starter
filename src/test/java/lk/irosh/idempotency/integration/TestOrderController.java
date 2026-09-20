package lk.irosh.idempotency.integration;

import lk.irosh.idempotency.annotation.Idempotent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/test/orders")
public class TestOrderController {

    private final AtomicInteger executionCount =
            new AtomicInteger(0);

    @Idempotent
    @PostMapping
    public Map<String, Object> createOrder(
            @RequestBody Map<String, Object> request
    ) {
        int count = executionCount.incrementAndGet();

        return Map.of(
                "orderId", 1001,
                "executionCount", count,
                "status", "CREATED"
        );
    }

    public int getExecutionCount() {
        return executionCount.get();
    }

    public void resetExecutionCount() {
        executionCount.set(0);
    }
}