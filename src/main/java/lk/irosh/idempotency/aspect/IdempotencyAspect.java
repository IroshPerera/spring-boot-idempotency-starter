package lk.irosh.idempotency.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lk.irosh.idempotency.annotation.Idempotent;
import lk.irosh.idempotency.model.IdempotencyRecord;
import lk.irosh.idempotency.service.IdempotencyService;
import lk.irosh.idempotency.util.RequestHashUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

@Aspect
public class IdempotencyAspect {

    private final IdempotencyService idempotencyService;

    private final ObjectMapper objectMapper;

    private final HttpServletRequest request;

    public IdempotencyAspect(
            IdempotencyService idempotencyService,
            ObjectMapper objectMapper,
            HttpServletRequest request
    ) {
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
        this.request = request;
    }

    @Around("@annotation(lk.irosh.idempotency.annotation.Idempotent)")
    public Object handleIdempotency(
            ProceedingJoinPoint joinPoint
    ) throws Throwable {

        Method method = getMethod(joinPoint);

        Idempotent annotation =
                method.getAnnotation(Idempotent.class);

        String idempotencyKey =
                request.getHeader(annotation.keyHeader());

        String requestHash =
                createRequestHash(joinPoint);

        Optional<IdempotencyRecord> existing =
                idempotencyService.reserve(
                        idempotencyKey,
                        requestHash,
                        request.getRequestURI(),
                        request.getMethod(),
                        java.time.Duration.ofSeconds(
                                annotation.expirySeconds()
                        )
                );

        if (existing.isPresent()) {
            return convertSavedResponse(
                    existing.get(),
                    method
            );
        }

        try {
            Object response = joinPoint.proceed();

            if (annotation.cacheResponse()) {
                saveResponse(
                        idempotencyKey,
                        response
                );
            }

            return response;

        } catch (Throwable exception) {
            idempotencyService.fail(
                    idempotencyKey
            );

            throw exception;
        }
    }

    private Method getMethod(
            ProceedingJoinPoint joinPoint
    ) {
        MethodSignature signature =
                (MethodSignature) joinPoint.getSignature();

        return signature.getMethod();
    }

    private String createRequestHash(
            ProceedingJoinPoint joinPoint
    ) {
        Object[] arguments = Arrays.stream(
                        joinPoint.getArgs()
                )
                .filter(argument ->
                        !(argument
                                instanceof HttpServletRequest)
                )
                .toArray();

        return RequestHashUtil.createHash(
                arguments,
                objectMapper
        );
    }

    private void saveResponse(
            String idempotencyKey,
            Object response
    ) throws Exception {

        int status = 200;
        Object body = response;

        if (response instanceof ResponseEntity<?> entity) {
            status = entity.getStatusCode().value();
            body = entity.getBody();
        }

        String responseBody =
                objectMapper.writeValueAsString(body);

        idempotencyService.complete(
                idempotencyKey,
                status,
                responseBody
        );
    }

    private Object convertSavedResponse(
            IdempotencyRecord record,
            Method method
    ) throws Exception {

        Class<?> returnType =
                method.getReturnType();

        if (returnType.equals(ResponseEntity.class)) {
            return ResponseEntity
                    .status(record.getHttpStatus())
                    .body(record.getResponseBody());
        }

        return objectMapper.readValue(
                record.getResponseBody(),
                returnType
        );
    }
}