package lk.irosh.idempotency.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.junit.jupiter.api.BeforeEach;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        classes = TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class IdempotencyIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TestOrderController controller;

    @Test
    void shouldReturnSameResponseForDuplicateRequest() {

        String url =
                "http://localhost:" + port + "/test/orders";

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        headers.set(
                "Idempotency-Key",
                "order-key-001"
        );

        Map<String, Object> requestBody =
                Map.of(
                        "productId", 10,
                        "quantity", 2
                );

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(
                        requestBody,
                        headers
                );

        ResponseEntity<String> firstResponse =
                restTemplate.postForEntity(
                        url,
                        request,
                        String.class
                );

        ResponseEntity<String> secondResponse =
                restTemplate.postForEntity(
                        url,
                        request,
                        String.class
                );

        assertEquals(
                HttpStatus.OK,
                firstResponse.getStatusCode()
        );

        assertEquals(
                HttpStatus.OK,
                secondResponse.getStatusCode()
        );

        assertEquals(
                firstResponse.getBody(),
                secondResponse.getBody()
        );

        assertEquals(
                1,
                controller.getExecutionCount()
        );
    }

    @Test
    void shouldRejectRequestWithoutIdempotencyKey() {

        String url =
                "http://localhost:" + port + "/test/orders";

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        Map<String, Object> requestBody =
                Map.of(
                        "productId", 20,
                        "quantity", 1
                );

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(
                        requestBody,
                        headers
                );

        ResponseEntity<String> response =
                restTemplate.postForEntity(
                        url,
                        request,
                        String.class
                );

        assertEquals(
                HttpStatus.BAD_REQUEST,
                response.getStatusCode()
        );
    }

    @Test
    void shouldRejectSameKeyWithDifferentRequestBody() {

        String url =
                "http://localhost:" + port + "/test/orders";

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(
                MediaType.APPLICATION_JSON
        );

        headers.set(
                "Idempotency-Key",
                "same-key-different-body"
        );

        Map<String, Object> firstBody =
                Map.of(
                        "productId", 10,
                        "quantity", 1
                );

        Map<String, Object> secondBody =
                Map.of(
                        "productId", 99,
                        "quantity", 5
                );

        HttpEntity<Map<String, Object>> firstRequest =
                new HttpEntity<>(
                        firstBody,
                        headers
                );

        HttpEntity<Map<String, Object>> secondRequest =
                new HttpEntity<>(
                        secondBody,
                        headers
                );

        restTemplate.postForEntity(
                url,
                firstRequest,
                String.class
        );

        ResponseEntity<String> secondResponse =
                restTemplate.postForEntity(
                        url,
                        secondRequest,
                        String.class
                );

        assertEquals(
                HttpStatus.CONFLICT,
                secondResponse.getStatusCode()
        );
    }

    @BeforeEach
    void resetControllerCount() {
        controller.resetExecutionCount();
    }
}