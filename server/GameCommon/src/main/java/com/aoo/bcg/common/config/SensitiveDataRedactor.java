package com.aoo.bcg.common.config;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Mandatory boundary sanitizer for startup diagnostics, exceptions and structured logs. */
public final class SensitiveDataRedactor {
    public static final String MASK = "[REDACTED]";
    private static final Set<String> SENSITIVE_KEYS = Set.of(
        "password", "passwd", "secret", "token", "authorization", "privatekey", "certificate", "connectionstring", "jdbcurl",
        "idcard", "identitycard", "identitynumber", "身份证", "privatecards", "handcards", "cardlist", "deck", "cardwall");
    private static final Pattern BEARER = Pattern.compile("(?i)(bearer\\s+)[A-Za-z0-9._~+/-]+=*");
    private static final Pattern ASSIGNMENT = Pattern.compile("(?i)(password|passwd|secret|token|private[_-]?key|client[_-]?secret)(\\s*[=:]\\s*)[^\\s,;]+", Pattern.MULTILINE);
    private static final Pattern JDBC_CREDENTIALS = Pattern.compile("(?i)(jdbc:[^\\s?]+\\?[^\\s]*?(?:user|username|password)=)[^&\\s]+", Pattern.MULTILINE);
    private static final Pattern PEM = Pattern.compile("-----BEGIN [^-]+-----[\\s\\S]*?-----END [^-]+-----");
    private static final Pattern IDENTITY_NUMBER = Pattern.compile("(?<!\\d)\\d{17}[0-9Xx](?!\\d)");

    private SensitiveDataRedactor() {}

    public static String redact(String value) {
        if (value == null) return null;
        String result = BEARER.matcher(value).replaceAll("$1" + MASK);
        result = ASSIGNMENT.matcher(result).replaceAll("$1$2" + MASK);
        result = JDBC_CREDENTIALS.matcher(result).replaceAll("$1" + MASK);
        result = PEM.matcher(result).replaceAll(MASK);
        return IDENTITY_NUMBER.matcher(result).replaceAll(MASK);
    }

    public static Map<String,Object> redact(Map<String,?> values) {
        Map<String,Object> result = new LinkedHashMap<>();
        values.forEach((key,value) -> result.put(key, sensitiveKey(key) ? MASK : sanitize(value)));
        return Map.copyOf(result);
    }

    public static String safeFailure(Throwable failure) {
        if (failure == null) return "unknown failure";
        return failure.getClass().getSimpleName() + ": " + redact(failure.getMessage());
    }

    private static Object sanitize(Object value) {
        if (value instanceof Map<?,?> map) {
            Map<String,Object> strings = new LinkedHashMap<>();
            map.forEach((key,item) -> strings.put(String.valueOf(key), item));
            return redact(strings);
        }
        return value instanceof CharSequence text ? redact(text.toString()) : value;
    }

    private static boolean sensitiveKey(String key) {
        String normalized = key.replace("_", "").replace("-", "").replace(".", "").toLowerCase(Locale.ROOT);
        return SENSITIVE_KEYS.stream().anyMatch(normalized::contains);
    }
}
