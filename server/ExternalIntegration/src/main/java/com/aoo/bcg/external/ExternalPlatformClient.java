package com.aoo.bcg.external;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

public final class ExternalPlatformClient {
    @FunctionalInterface interface Sleeper { void sleep(Duration duration) throws InterruptedException; }
    private final ExternalPlatformConfig config;
    private final SecretProvider secrets;
    private final HttpClient http;
    private final CircuitBreaker circuit;
    private final Sleeper sleeper;

    public ExternalPlatformClient(ExternalPlatformConfig config, SecretProvider secrets) {
        this(config, secrets, HttpClient.newBuilder().connectTimeout(config.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER).build(), Clock.systemUTC(), d -> Thread.sleep(d));
    }

    ExternalPlatformClient(ExternalPlatformConfig config, SecretProvider secrets, HttpClient http, Clock clock, Sleeper sleeper) {
        this.config = Objects.requireNonNull(config);
        this.secrets = Objects.requireNonNull(secrets);
        this.http = Objects.requireNonNull(http);
        this.circuit = new CircuitBreaker(config.circuitFailureThreshold(), config.circuitOpenDuration(), clock);
        this.sleeper = Objects.requireNonNull(sleeper);
    }

    public ExternalResponse execute(ExternalRequest request) {
        Objects.requireNonNull(request);
        circuit.acquire();
        try {
            ExternalResponse response = attempt(request);
            circuit.success();
            return response;
        } catch (ExternalPlatformException.Protocol | ExternalPlatformException.Transport failure) {
            circuit.failure();
            throw failure;
        }
    }

    private ExternalResponse attempt(ExternalRequest request) {
        for (int attempt = 1; attempt <= config.maxAttempts(); attempt++) {
            try {
                HttpResponse<byte[]> raw = http.send(toHttpRequest(request), HttpResponse.BodyHandlers.ofByteArray());
                ExternalResponse response = new ExternalResponse(raw.statusCode(), raw.headers().map(), raw.body());
                if (response.successful()) return response;
                ExternalPlatformException.Protocol failure = new ExternalPlatformException.Protocol(response.statusCode());
                if (!canRetry(request, attempt, response.statusCode())) throw failure;
            } catch (IOException e) {
                if (!canRetry(request, attempt, 0)) throw new ExternalPlatformException.Transport("external platform transport failed", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ExternalPlatformException.Transport("external platform call interrupted", e);
            }
            backoff(attempt);
        }
        throw new IllegalStateException("unreachable");
    }

    private HttpRequest toHttpRequest(ExternalRequest request) {
        URI target = config.baseUri().resolve(request.relativePath());
        if (!Objects.equals(target.getHost(), config.baseUri().getHost())) throw new IllegalArgumentException("resolved host differs from configured host");
        HttpRequest.Builder builder = HttpRequest.newBuilder(target).timeout(config.requestTimeout())
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + secrets.apiKey());
        request.headers().forEach(builder::header);
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) builder.header("Idempotency-Key", request.idempotencyKey());
        return builder.method(request.method(), request.publisher()).build();
    }

    private boolean canRetry(ExternalRequest request, int attempt, int status) {
        if (attempt >= config.maxAttempts() || !request.retrySafe()) return false;
        return status == 0 || status == 408 || status == 429 || status == 502 || status == 503 || status == 504;
    }

    private void backoff(int failedAttempt) {
        long multiplier = 1L << Math.min(20, failedAttempt - 1);
        Duration delay;
        try { delay = config.initialBackoff().multipliedBy(multiplier); }
        catch (ArithmeticException overflow) { delay = config.requestTimeout(); }
        if (delay.compareTo(config.requestTimeout()) > 0) delay = config.requestTimeout();
        try { sleeper.sleep(delay); }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExternalPlatformException.Transport("external platform retry interrupted", e);
        }
    }
}
