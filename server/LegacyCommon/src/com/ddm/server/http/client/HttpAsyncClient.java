package com.ddm.server.http.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletionException;

/**
 * Http异步请求
 */
public class HttpAsyncClient {
    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public static void startHttpGet(String request, final IResponseHandler response) {
        send(HttpRequest.newBuilder(URI.create(request)).timeout(TIMEOUT).GET().build(), response);
    }

    public static void startHttpPost(String url, String json, final IResponseHandler response) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json;charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();
        send(request, response);
    }

    private static void send(HttpRequest request, IResponseHandler handler) {
        CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .whenComplete((result, error) -> {
                    if (error != null) {
                        Throwable cause = error instanceof CompletionException && error.getCause() != null
                                ? error.getCause() : error;
                        handler.failed(cause instanceof Exception ? (Exception) cause : new RuntimeException(cause));
                        return;
                    }
                    handler.completed(result.body());
                });
    }
}
