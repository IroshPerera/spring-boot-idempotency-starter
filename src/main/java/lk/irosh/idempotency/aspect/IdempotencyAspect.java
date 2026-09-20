package lk.irosh.idempotency.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lk.irosh.idempotency.annotation.Idempotent;
import lk.irosh.idempotency.config.IdempotencyProperties;
import lk.irosh.idempotency.model.IdempotencyRecord;
import lk.irosh.idempotency.service.IdempotencyReservation;
import lk.irosh.idempotency.service.IdempotencyService;
import lk.irosh.idempotency.util.RequestHashUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Method;
import java.util.Arrays;

@Aspect
public class IdempotencyAspect {

    private final IdempotencyService idempotencyService;

    private final ObjectMapper objectMapper;

    private final HttpServletRequest request;

    private final IdempotencyProperties properties;

    public IdempotencyAspect(
            IdempotencyService idempotencyService,
            ObjectMapper objectMapper,
            HttpServletRequest request,
            IdempotencyProperties properties
    ) {
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
        this.request = request;
        this.properties = properties;
    }

    @Around("@annotation(lk.irosh.idempotency.annotation.Idempotent)")
    public Object handleIdempotency(
            ProceedingJoinPoint joinPoint
    ) throws Throwable {

        Method method = getMethod(joinPoint);

        Idempotent annotation =
                method.getAnnotation(Idempotent.class);

        String headerName = annotation.keyHeader().isBlank()
                ? properties.getHeaderName()
                : annotation.keyHeader();

        String idempotencyKey = request.getHeader(headerName);

        if ((idempotencyKey == null || idempotencyKey.isBlank())
                && !properties.isRequired()) {
            return joinPoint.proceed();
        }

        String requestHash =
                createRequestHash(joinPoint);

        java.time.Duration ttl = annotation.expirySeconds() > 0
                ? java.time.Duration.ofSeconds(annotation.expirySeconds())
                : properties.getDefaultExpiry();

        IdempotencyReservation reservation =
                idempotencyService.reserveRequest(
                        idempotencyKey,
                        requestHash,
                        request.getRequestURI(),
                        request.getMethod(),
                        ttl
                );

        if (reservation.isReplay()) {
            return convertSavedResponse(
                    reservation.completedRecord().orElseThrow(),
                    method
            );
        }

        try {
            Object response = joinPoint.proceed();

            if (annotation.cacheResponse()
                    && properties.isCacheResponse()) {
                saveResponse(
                        idempotencyKey,
                        reservation.reservationId(),
                        response
                );
            } else {
                idempotencyService.release(
                        idempotencyKey,
                        reservation.reservationId()
                );
            }

            return response;

        } catch (Throwable exception) {
            idempotencyService.fail(
                    idempotencyKey,
                    reservation.reservationId()
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
            String reservationId,
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
                reservationId,
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
