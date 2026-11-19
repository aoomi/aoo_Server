package com.aoo.bcg.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Authorizes an uploaded voice asset for every authoritative room member before dispatch. */
final class GatewayVoiceAssetAuthority {
  private static final ObjectMapper JSON = new ObjectMapper();
  private static final HttpClient HTTP =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

  private GatewayVoiceAssetAuthority() {}

  static Map<String, Object> authorize(
      Map<String, Object> commandBody, long senderId, Map<String, Object> roomState) {
    if (!"voice".equals(String.valueOf(commandBody.get("action")))) return commandBody;
    try {
      long assetId = positive(commandBody.get("assetId"), "assetId");
      List<Long> members = roomMembers(roomState);
      byte[] requestBody =
          JSON.writeValueAsBytes(
              Map.of("senderId", senderId, "assetId", assetId, "memberIds", members));
      HttpRequest request =
          HttpRequest.newBuilder(
                  URI.create(requiredEnv("AOO_MEDIA_INTERNAL_URL"))
                      .resolve("/internal/media/voice/authorize"))
              .header("Authorization", "Bearer " + requiredEnv("AOO_MEDIA_ROOM_TOKEN"))
              .header("Content-Type", "application/json")
              .timeout(Duration.ofSeconds(3))
              .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
              .build();
      HttpResponse<byte[]> response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
      if (response.statusCode() != 200)
        throw new SecurityException("voice asset authorization rejected");
      @SuppressWarnings("unchecked")
      Map<String, Object> root = JSON.readValue(response.body(), Map.class);
      @SuppressWarnings("unchecked")
      Map<String, Object> data = (Map<String, Object>) root.get("data");
      if (data == null || !"READY".equals(data.get("state")))
        throw new SecurityException("voice asset is not READY");
      Map<String, Object> authoritative = new LinkedHashMap<>(commandBody);
      authoritative.put("assetId", assetId);
      authoritative.put("durationMs", positive(data.get("durationMillis"), "durationMillis"));
      authoritative.put("mimeType", String.valueOf(data.get("mimeType")));
      return Map.copyOf(authoritative);
    } catch (SecurityException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalStateException("MEDIA_AUTHORITY_UNAVAILABLE", e);
    }
  }

  private static List<Long> roomMembers(Map<String, Object> roomState) {
    Object rawPlayers = roomState.get("players");
    if (!(rawPlayers instanceof Map<?, ?> players))
      throw new IllegalStateException("authoritative room members unavailable");
    List<Long> members =
        players.values().stream().map(v -> positive(v, "room member")).distinct().toList();
    if (members.isEmpty()) throw new IllegalStateException("authoritative room members unavailable");
    return members;
  }

  private static long positive(Object value, String field) {
    long number = value instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(value));
    if (number <= 0) throw new IllegalArgumentException(field + " required");
    return number;
  }

  private static String requiredEnv(String name) {
    String value = System.getenv(name);
    if (value == null || value.isBlank()) throw new IllegalStateException(name + " is required");
    return value;
  }
}
