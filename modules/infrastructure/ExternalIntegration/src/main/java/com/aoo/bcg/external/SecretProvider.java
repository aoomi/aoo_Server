package com.aoo.bcg.external;

@FunctionalInterface
public interface SecretProvider {
    String apiKey();

    static SecretProvider environment(String variableName) {
        if (variableName == null || variableName.isBlank()) throw new IllegalArgumentException("secret environment variable is required");
        return () -> {
            String value = System.getenv(variableName);
            if (value == null || value.isBlank()) throw new ExternalPlatformException.Configuration("required secret is unavailable");
            return value;
        };
    }
}
