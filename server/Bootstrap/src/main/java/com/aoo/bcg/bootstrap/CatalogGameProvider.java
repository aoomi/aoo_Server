package com.aoo.bcg.bootstrap;

import com.aoo.bcg.gamespi.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.aoo.bcg.gamespi.time.OperationDeadline;
import com.aoo.bcg.gamespi.time.OperationDeadlineArbiter;

/** Metadata-driven provider for legacy games while their native implementation is migrated. */
final class CatalogGameProvider implements GameProvider {
    private final GameDescriptor descriptor;
    private final Map<String, Object> defaults;

    CatalogGameProvider(GameDescriptor descriptor, boolean enabled, String sourceModule) {
        this.descriptor = descriptor;
        this.defaults = Map.of("enabled", enabled, "playVersion", descriptor.version(),
                "sourceModule", sourceModule, "migrationMode", "catalog-bridge");
    }

    @Override public GameDescriptor descriptor() { return descriptor; }
    @Override public Map<String, Object> defaultConfiguration() { return defaults; }
    @Override public GameRoomFactory roomFactory() {
        return context -> new GameRoomHandle(context.roomId(), descriptor.gameId(), descriptor.version(),
                new CatalogGameSession(context.roomId(), context.ownerId(), descriptor, context.immutableRules()));
    }
    @Override public Optional<GameCommandHandler> commandHandler() {
        return Optional.of(new AuthoritativeSessionCommandHandler());
    }
    @Override public Optional<ReconnectViewProvider<?>> reconnectViewProvider() {
        return Optional.of((viewer, room) -> room.requireAuthoritativeSession().viewFor(viewer));
    }
    @Override public Optional<SettlementProvider> settlementProvider() {
        return Optional.of((room, round) -> room.requireAuthoritativeSession().settlement(round, room.playVersion()));
    }
    @Override public List<RuleComponent<GameCommandRequest>> ruleComponents() {
        return List.of(new RuleComponent<>() {
            @Override public String ruleId() { return "catalog." + descriptor.code() + ".boundary"; }
            @Override public String componentVersion() { return descriptor.version(); }
            @Override public RuleStage stage() { return RuleStage.ROOM; }
            @Override public int priority() { return 0; }
            @Override public RuleResult execute(GameCommandRequest request) {
                if (request.playVersion().equals(descriptor.version())
                        && (request.msgId().startsWith(categoryPrefix()) || request.msgId().startsWith("common.room."))) {
                    return RuleResult.accept();
                }
                return RuleResult.reject("GAME_BOUNDARY_MISMATCH", "command does not belong to registered game");
            }
        });
    }
    private String categoryPrefix() {
        return switch (descriptor.category()) {
            case MAHJONG -> "mahjong.";
            case POKER -> "poker.";
            case LONG_CARD -> "long_card.";
            case WORD_CARD -> "word_card.";
        };
    }

    private static final class CatalogGameSession implements AuthoritativeGameSession {
        private final long roomId;
        private final long ownerId;
        private final GameDescriptor descriptor;
        private final Map<String, Object> rules;
        private final Map<Integer, Long> seats = new LinkedHashMap<>();
        private long lastSequence;
        private long stateVersion;
        private boolean started;
        private final OperationDeadlineArbiter deadlineArbiter = new OperationDeadlineArbiter();

        private CatalogGameSession(long roomId, long ownerId, GameDescriptor descriptor, Map<String, Object> rules) {
            this.roomId = roomId; this.ownerId = ownerId; this.descriptor = descriptor; this.rules = Map.copyOf(rules);
            seats.put(0, ownerId);
        }
        @Override public synchronized GameCommandResult execute(GameCommandRequest request) {
            long playerId = parsePlayer(request.authenticatedUserId());
            if (request.sequence() <= lastSequence) throw new IllegalArgumentException("sequence must increase");
            if (request.seatId() < 0 || request.seatId() > 7) throw new IllegalArgumentException("invalid seat");
            String action = request.msgId().substring(request.msgId().lastIndexOf('.') + 1);
            switch (action) {
                case "join" -> {
                    if (started) throw new IllegalStateException("room already started");
                    Long occupant = seats.putIfAbsent(request.seatId(), playerId);
                    if (occupant != null && occupant != playerId) throw new IllegalStateException("seat occupied");
                }
                case "start" -> {
                    requireSeat(request.seatId(), playerId);
                    if (playerId != ownerId) throw new IllegalArgumentException("only owner may start");
                    started = true;
                }
                case "leave" -> {
                    requireSeat(request.seatId(), playerId);
                    if (started) throw new IllegalStateException("cannot leave started room");
                    seats.remove(request.seatId());
                }
                default -> throw new IllegalStateException("native gameplay is not enabled for catalog bridge: " + descriptor.code());
            }
            lastSequence = request.sequence();
            stateVersion = Math.addExact(stateVersion, 1);
            return new GameCommandResult(request.msgId() + "_resp", request.requestId(), viewFor(playerId));
        }
        @Override public synchronized Map<String, Object> viewFor(long viewerPlayerId) {
            return Map.of("gameId", descriptor.gameId(), "gameCode", descriptor.code(), "started", started,
                    "seats", Map.copyOf(seats), "viewerSeated", seats.containsValue(viewerPlayerId));
        }
        @Override public synchronized Map<String, Object> authoritativeState() {
            return Map.of("gameId", descriptor.gameId(), "ownerId", ownerId, "started", started,
                    "seats", Map.copyOf(seats), "rules", rules, "lastSequence", lastSequence);
        }
        @Override public synchronized long stateVersion() { return stateVersion; }
        @Override public OperationDeadline operationDeadline() { return OperationDeadline.none(); }
        @Override public OperationDeadlineArbiter deadlineArbiter() { return deadlineArbiter; }
        @Override public synchronized List<String> invariantViolations() {
            List<String> violations = new java.util.ArrayList<>();
            if (roomId <= 0) violations.add("roomId must be positive");
            if (ownerId <= 0) violations.add("ownerId must be positive");
            if (!seats.containsValue(ownerId)) violations.add("owner must remain seated");
            if (seats.values().stream().distinct().count() != seats.size()) violations.add("player occupies multiple seats");
            return List.copyOf(violations);
        }
        @Override public synchronized SettlementPayload settlement(int roundNo, String playVersion) {
            if (!started || seats.isEmpty()) throw new IllegalStateException("room has no completed round");
            Map<Long, Long> zeroScores = new LinkedHashMap<>();
            seats.values().forEach(player -> zeroScores.put(player, 0L));
            return new SettlementPayload(roomId, roundNo, playVersion, zeroScores);
        }
        private void requireSeat(int seatId, long playerId) {
            if (!Long.valueOf(playerId).equals(seats.get(seatId))) throw new IllegalArgumentException("seat ownership mismatch");
        }
        private static long parsePlayer(String value) {
            try { long id = Long.parseLong(value); if (id > 0) return id; }
            catch (NumberFormatException ignored) { /* Invalid identity is rejected uniformly below. */ }
            throw new IllegalArgumentException("authenticated user must be a positive numeric id");
        }
    }
}
