package com.aoo.bcg.gateway;

import com.aoo.bcg.gamespi.time.AuthoritativeTimeSource;

import java.util.Map;
import java.util.Objects;

/** The only HTTP response envelope exposed by the v2 Gateway. */
public record HttpResult<T>(int code, String msg, T data, String traceId, long timestamp) {
    public HttpResult {
        if (code < 0 || msg == null || msg.isBlank() || traceId == null || traceId.isBlank()
                || timestamp <= 0) {
            throw new IllegalArgumentException("invalid HTTP response envelope");
        }
        Objects.requireNonNull(data, "HTTP response data must never be null");
    }

    public static <T> HttpResult<T> success(T data, String traceId) {
        return success(data, traceId, AuthoritativeTimeSource.systemUtc());
    }

    public static <T> HttpResult<T> success(T data, String traceId, AuthoritativeTimeSource time) {
        Objects.requireNonNull(time, "time");
        return new HttpResult<>(0, "success", data, traceId, time.epochMillis());
    }

    /** Use an object rather than null for successful operations with no domain payload. */
    public static HttpResult<Map<String, Object>> emptySuccess(String traceId) {
        return success(Map.of(), traceId);
    }

    public static <T> HttpResult<T> failure(GatewayErrorCode error, T data, String traceId,
                                             AuthoritativeTimeSource time) {
        Objects.requireNonNull(error, "error");
        Objects.requireNonNull(time, "time");
        return new HttpResult<>(error.code(), error.defaultMessage(), data, traceId, time.epochMillis());
    }
}
