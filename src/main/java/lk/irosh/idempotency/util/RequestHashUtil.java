package lk.irosh.idempotency.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class RequestHashUtil {

    private RequestHashUtil() {
    }

    public static String createHash(
            Object requestBody,
            ObjectMapper objectMapper
    ) {
        try {
            String json = objectMapper.writeValueAsString(
                    requestBody
            );

            return sha256(json);

        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Unable to create request hash",
                    exception
            );
        }
    }

    private static String sha256(String value) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hashBytes = digest.digest(
                    value.getBytes(StandardCharsets.UTF_8)
            );

            StringBuilder result = new StringBuilder();

            for (byte hashByte : hashBytes) {
                result.append(
                        String.format(
                                "%02x",
                                hashByte
                        )
                );
            }

            return result.toString();

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    exception
            );
        }
    }
}