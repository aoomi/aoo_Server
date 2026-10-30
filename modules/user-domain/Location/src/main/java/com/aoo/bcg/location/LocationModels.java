package com.aoo.bcg.location;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class LocationModels {
    private LocationModels() {}

    public enum AuthorizationStatus { GRANTED, DENIED, RESTRICTED, NOT_DETERMINED }
    public enum SignalKind { IPV4, IPV6, DEVICE_ID, WIFI_BSSID }
    public enum RiskReason { GPS_PROXIMITY, SHARED_IP, SHARED_DEVICE, LOCATION_UNAVAILABLE, LOW_LOCATION_ACCURACY }

    public record AuthorizationReport(String eventId, long playerId, AuthorizationStatus status,
                                      Instant occurredAt, String platform, String platformDetail) {
        public AuthorizationReport { eventId = text(eventId, "eventId", 64); positive(playerId, "playerId"); Objects.requireNonNull(status); Objects.requireNonNull(occurredAt); platform = text(platform, "platform", 32); platformDetail = optional(platformDetail, 256); }
    }

    public record GeoPoint(double latitude, double longitude, double accuracyMeters, Instant capturedAt) {
        public GeoPoint {
            if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) invalid("latitude out of range");
            if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) invalid("longitude out of range");
            if (!Double.isFinite(accuracyMeters) || accuracyMeters < 0) invalid("accuracyMeters must be non-negative");
            Objects.requireNonNull(capturedAt, "capturedAt");
        }
    }

    public record ParticipantLocation(long playerId, GeoPoint point) {
        public ParticipantLocation { positive(playerId, "playerId"); }
    }

    public record TableRiskRequest(String tableId, List<Long> participantIds, List<ParticipantLocation> locations,
                                   Instant evaluatedAt) {
        public TableRiskRequest {
            tableId = text(tableId, "tableId", 128); participantIds = List.copyOf(Objects.requireNonNull(participantIds)); locations = List.copyOf(Objects.requireNonNull(locations)); Objects.requireNonNull(evaluatedAt);
            if (participantIds.size() < 2 || Set.copyOf(participantIds).size() != participantIds.size()) invalid("at least two unique participants required");
        }
    }

    public record PairRisk(long firstPlayerId, long secondPlayerId, Double distanceMeters,
                           int score, Set<RiskReason> reasons) {
        public PairRisk { reasons = Set.copyOf(reasons); }
    }

    public record TableRiskAssessment(String assessmentId, String tableId, int apiVersion,
                                      int maxScore, List<PairRisk> pairs, Instant evaluatedAt) {
        public TableRiskAssessment { pairs = List.copyOf(pairs); }
    }

    public record RiskSignal(String type, String valueHash) {
        public RiskSignal { type = text(type, "signal.type", 32); valueHash = text(valueHash, "signal.valueHash", 128); }
    }

    public record NetworkRiskReport(String eventId, long playerId, Instant observedAt,
                                    List<RiskSignal> ipSignals, List<RiskSignal> deviceSignals) {
        public NetworkRiskReport { eventId = text(eventId, "eventId", 64); positive(playerId, "playerId"); Objects.requireNonNull(observedAt); ipSignals = List.copyOf(ipSignals); deviceSignals = List.copyOf(deviceSignals); if (ipSignals.isEmpty() && deviceSignals.isEmpty()) invalid("at least one signal required"); }
    }

    static String text(String value, String name, int max) { if (value == null || value.isBlank() || value.length() > max) invalid(name + " is invalid"); return value; }
    static String optional(String value, int max) { if (value != null && value.length() > max) invalid("text too long"); return value; }
    static void positive(long value, String name) { if (value <= 0) invalid(name + " must be positive"); }
    static void invalid(String message) { throw new LocationServiceException(LocationErrorCode.INVALID_ARGUMENT, message); }
}
