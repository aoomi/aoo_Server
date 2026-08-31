package core.server;

import business.global.room.RoomMgr;
import business.global.room.base.AbsBaseRoom;
import business.global.room.base.AbsRoomPos;
import business.player.Player;
import cenum.ChatType;
import com.aoo.bcg.common.idempotency.IdempotencyKey;
import com.aoo.bcg.common.idempotency.IdempotencyStore;
import com.aoo.bcg.gamespi.GameCommandResult;
import com.aoo.bcg.common.invite.InvitePayload;
import com.aoo.bcg.common.invite.SignedInviteService;
import com.ddm.server.protocol.v2.ProtocolV2AuthorityRuntime;
import com.ddm.server.websocket.def.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.network.http.proto.SData_Result;

import java.time.Clock;
import java.time.Duration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Production authority for game-independent room realtime commands. */
final class ProductionRoomRealtimeService {
    private static final Set<String> COMMANDS = Set.of("join", "state", "leave", "ready", "unready", "start",
            "settings", "quick_text", "voice", "magic_expression", "invite", "dissolve_apply",
            "dissolve_vote", "trustee", "heartbeat", "reconnect", "shuffle", "kick");
    private static final Duration RETENTION = Duration.ofHours(24);
    private final IdempotencyStore<GameCommandResult> idempotency;
    private final ObjectMapper mapper;
    private final Clock clock;
    private final ProductionRoomAdministrationService administration;
    private final ProductionRoomReconnectService reconnect;
    private final com.aoo.bcg.common.event.JdbcRoomEventJournal authorityJournal;
    private final com.aoo.bcg.common.recovery.JdbcRoomSnapshotStore authoritySnapshots;
    private final ConcurrentHashMap<String, Window> rateWindows = new ConcurrentHashMap<>();

    ProductionRoomRealtimeService(IdempotencyStore<GameCommandResult> idempotency, ObjectMapper mapper, Clock clock,
                                  com.aoo.bcg.gamespi.GameRegistry games, javax.sql.DataSource dataSource) {
        this.idempotency = idempotency; this.mapper = mapper; this.clock = clock;
        this.administration = new ProductionRoomAdministrationService(games);
        this.reconnect = new ProductionRoomReconnectService(dataSource, mapper);
        this.authorityJournal = new com.aoo.bcg.common.event.JdbcRoomEventJournal(dataSource, mapper);
        this.authoritySnapshots = new com.aoo.bcg.common.recovery.JdbcRoomSnapshotStore(dataSource, mapper);
    }

    boolean supports(String messageId) {
        return messageId.startsWith("common.room.") || messageId.startsWith("room.");
    }

    GameCommandResult dispatch(Player player, ProtocolV2AuthorityRuntime.Command command) {
        long roomId = parseRoomId(command.roomId());
        String action = action(command);
        if (!COMMANDS.contains(action)) throw new IllegalArgumentException("unsupported room command: " + action);
        IdempotencyKey key = new IdempotencyKey(Long.toString(player.getPid()), "room." + action,
                roomId, command.roundNo(), command.requestId());
        GameCommandResult prior = idempotency.find(key).orElse(null);
        if (prior != null) return prior;
        if (!idempotency.acquire(key, RETENTION)) {
            prior = idempotency.find(key).orElse(null);
            if (prior != null) return prior;
            throw new IllegalStateException("REQUEST_OUTCOME_UNKNOWN:" + command.requestId());
        }
        boolean mutationStarted = false;
        try {
            if ("reconnect".equals(action)) {
                GameCommandResult result = new GameCommandResult("room.reconnect_resp", command.requestId(),
                        reconnect.reconnect(player, command)).withTiming(clock.millis(),
                        com.aoo.bcg.gamespi.time.OperationDeadline.none());
                idempotency.save(key, result, RETENTION);
                return result;
            }
            AbsBaseRoom room = requireRoom(roomId);
            AbsRoomPos seat = room.getRoomPosMgr().getPosByPid(player.getPid());
            if (seat == null) throw new SecurityException("player is not seated in room");
            requireSeat(command.body(), seat.getPosID());
            throttle(player.getPid(), action);
            mutationStarted = isMutation(action);
            long persistedVersion=authoritySnapshots.latest(roomId)
                    .orElseThrow(()->new IllegalStateException("ROOM_AUTHORITY_SNAPSHOT_MISSING")).stateVersion();
            long expected=expectedStateVersion(command.body());
            if(expected!=persistedVersion)throw new java.util.ConcurrentModificationException("ROOM_STATE_VERSION_CONFLICT");
            long responseVersion=mutationStarted?Math.addExact(persistedVersion,1):persistedVersion;
            Map<String,Object> body = execute(room, seat, player, command, action, responseVersion);
            if(mutationStarted)persistMutation(room,player,command,action,expected,responseVersion,body);
            GameCommandResult result = new GameCommandResult("room." + action + "_resp", command.requestId(), body)
                    .withTiming(clock.millis(), com.aoo.bcg.gamespi.time.OperationDeadline.none());
            idempotency.save(key, result, RETENTION);
            return result;
        } catch (RuntimeException failure) {
            if (mutationStarted) idempotency.markUnknown(key); else idempotency.release(key);
            throw failure;
        }
    }

    private Map<String,Object> execute(AbsBaseRoom room, AbsRoomPos seat, Player player,
            ProtocolV2AuthorityRuntime.Command command, String action,long version) {
        Map<String,Object> body = command.body();
        if ("shuffle".equals(action) || "kick".equals(action))
            return administration.execute(player, room, command, action,Math.subtractExact(version,1),version).body().asMap();
        return switch (action) {
            case "join", "state" -> Map.of("viewerSnapshot", snapshot(room, player.getPid()),
                    "events", java.util.List.of(), "hasMore", false, "stateVersion", version,
                    "serverSeq", command.sequence(), "reconnectToken", command.requestId());
            case "heartbeat" -> Map.of("serverTime", clock.millis(), "serverSeq", command.sequence(),
                    "stateVersion", version);
            case "ready", "unready" -> success(room.playerReady(action.equals("ready"), player.getPid()),
                    Map.of("accepted", true, "ready", action.equals("ready"), "stateVersion", version, "serverSeq", command.sequence()));
            case "start" -> success(room.startGame(player.getPid()),
                    Map.of("accepted", true, "started", true, "stateVersion", version, "serverSeq", command.sequence()));
            case "leave" -> success(room.exitRoom(player.getPid()), Map.of("left", true, "stateVersion", version));
            case "trustee" -> success(room.opRoomTrusteeship(player.getPid(), bool(body, "enabled")),
                    Map.of("accepted", true, "trusteeship", bool(body, "enabled"), "stateVersion", version));
            case "quick_text" -> {
                int quickId = integer(body, "quickId", 0, 9999);
                yield success(room.opChat(player, "", ChatType.CHATTYPE_ROOM, room.getRoomID(), quickId),
                        Map.of("accepted", true, "quickId", quickId, "stateVersion", version));
            }
            case "voice" -> {
                long assetId = positiveLong(body, "assetId");
                java.util.List<Long> members=room.getRoomPosMgr().getPosList().stream().filter(java.util.Objects::nonNull).map(AbsRoomPos::getPid).filter(id->id>0).distinct().toList();
                VoiceAsset voice=authorizeVoice(player.getPid(),assetId,members);
                yield success(room.opRoomVoice(player.getPid(), "media:"+assetId), Map.of("accepted", true,
                        "assetId", assetId, "durationMs", voice.durationMillis(), "mimeType", voice.mimeType(), "state", "READY", "stateVersion", version));
            }
            case "magic_expression" -> {
                int expressionId = integer(body, "expressionId", 1, 9999);
                int targetSeatId = integer(body, "targetSeatId", 0, 99);
                if (room.getRoomPosMgr().getPosByPosID(targetSeatId) == null) throw new IllegalArgumentException("target seat does not exist");
                yield success(room.opChat(player, "", ChatType.CHATTYPE_ROOM, room.getRoomID(), expressionId),
                        Map.of("accepted", true, "expressionId", expressionId, "targetSeatId", targetSeatId, "stateVersion", version));
            }
            case "dissolve_apply" -> success(room.dissolveRoom(player.getPid()), Map.of("accepted", true,
                    "voteId", command.requestId(), "expiresAt", clock.millis() + 120_000, "stateVersion", version));
            case "dissolve_vote" -> {
                text(body, "voteId", 1, 128);
                boolean agree = bool(body, "agree");
                yield success(agree ? room.dissolveRoomAgree(player.getPid()) : room.dissolveRoomRefuse(player.getPid()),
                        Map.of("accepted", true, "agree", agree, "stateVersion", version));
            }
            case "invite" -> {
                long expiresAt = clock.millis() + 600_000;
                String inviteId = command.requestId().replaceAll("[^A-Za-z0-9_-]", "_");
                if (inviteId.length() < 16) inviteId = (inviteId + "________________").substring(0, 16);
                if (inviteId.length() > 64) inviteId = inviteId.substring(0, 64);
                byte[] key = inviteSigningKey();
                var signed = new SignedInviteService(key,
                        com.aoo.bcg.gamespi.time.AuthoritativeTimeSource.systemUtc()).sign(new InvitePayload(
                        room.getRoomID(), room.getBaseRoomConfigure().getGameType().getId(),
                        Math.max(0, room.getSpecialRoomId()), inviteId, command.playVersion(),
                        java.time.Instant.ofEpochMilli(expiresAt)));
                yield Map.of("accepted", true, "roomId", Long.toString(room.getRoomID()), "inviteId", inviteId,
                        "payload", signed.payload(), "signature", signed.signature(), "expiresAt", expiresAt, "stateVersion", version);
            }
            case "settings" -> {
                if (room.getOwnerID() != player.getPid())
                    throw new SecurityException("only room owner may change settings");
                @SuppressWarnings("unchecked") Map<String,Object> settings = body.get("settings") instanceof Map<?,?> raw
                        ? (Map<String,Object>) raw : throwInvalid("settings must be an object");
                if (settings.size() > 32) throw new IllegalArgumentException("too many settings");
                yield Map.of("accepted", true, "settings", Map.copyOf(settings), "stateVersion", version);
            }
            default -> throw new IllegalStateException("unreachable room action");
        };
    }

    private void persistMutation(AbsBaseRoom room,Player player,ProtocolV2AuthorityRuntime.Command command,
                                 String action,long expected,long next,Map<String,Object> response){
        var previous=authoritySnapshots.latest(room.getRoomID()).orElseThrow();
        Map<String,Object> state=new java.util.LinkedHashMap<>();
        state.put("room",snapshot(room,player.getPid()));state.put("stateVersion",next);
        state.put("_lastCommittedRequestId",command.requestId());state.put("_lastCommittedMsgId",command.msgId());
        state.put("_lastCommittedResultMsgId","room."+action+"_resp");state.put("_lastCommittedResultBody",response);
        state.put("_lastCommittedServerTime",clock.millis());
        var saved=new com.aoo.bcg.common.recovery.RoomSnapshot(previous.roomId(),previous.gameId(),previous.playVersion(),
                previous.componentVersion(),previous.fencingToken(),next,next,clock.instant(),state);
        var identity=com.aoo.bcg.common.event.RoomEventIdentity.v1(previous.roomId(),command.roundNo(),next,
                command.requestId(),command.msgId());
        Map<String,Object> payload=Map.of("action",action,"response",response,"stateAfter",state);
        authorityJournal.appendAndSnapshotAndOutboxCas(saved,identity,payload,
                new com.aoo.bcg.common.event.OutboxEvent("room:"+previous.roomId()+":"+next,"ROOM",previous.roomId(),
                        "ROOM_COMMAND_COMMITTED",1,payload,clock.instant()),expected);
    }

    private static long expectedStateVersion(Map<String,Object> body){Object value=body.get("expectedStateVersion");
        if(!(value instanceof Number number)||number.longValue()<0)throw new IllegalArgumentException("expectedStateVersion is required");
        return number.longValue();}

    private Map<String,Object> snapshot(AbsBaseRoom room, long playerId) {
        Map<String,Object> raw = mapper.convertValue(room.getRoomInfo(playerId), Map.class);
        return sanitize(raw);
    }

    private static Map<String,Object> sanitize(Map<?,?> source) {
        Map<String,Object> clean = new java.util.LinkedHashMap<>();
        for (var entry : source.entrySet()) {
            String key = String.valueOf(entry.getKey());
            if (Set.of("password", "passwordDES", "gps", "location", "longitude", "latitude").contains(key)) continue;
            Object value = entry.getValue();
            if (value instanceof Map<?,?> nested) value = sanitize(nested);
            else if (value instanceof java.util.List<?> list) value = list.stream().map(item -> item instanceof Map<?,?> nested ? sanitize(nested) : item).toList();
            clean.put(key, value);
        }
        return Map.copyOf(clean);
    }

    private static String action(ProtocolV2AuthorityRuntime.Command command) {
        String id = command.msgId();
        if (id.equals("room.leave")) return "leave";
        if (id.equals("room.reconnect")) return "reconnect";
        if (id.equals("room.dissolve_apply")) return "dissolve_apply";
        if (id.equals("room.dissolve_vote")) return "dissolve_vote";
        Object value = command.body().get("action");
        if (!(value instanceof String text) || text.isBlank()) throw new IllegalArgumentException("room action is required");
        return text.trim().toLowerCase(Locale.ROOT);
    }

    private void throttle(long playerId, String action) {
        long now = clock.millis(); int limit = switch (action) { case "heartbeat" -> 30; case "quick_text", "voice", "magic_expression" -> 8; default -> 20; };
        String key = playerId + ":" + action;
        rateWindows.compute(key, (ignored, old) -> {
            Window next = old == null || now - old.startedAt >= 10_000 ? new Window(now, 1) : new Window(old.startedAt, old.count + 1);
            if (next.count > limit) throw new IllegalStateException("ROOM_RATE_LIMITED"); return next;
        });
    }

    private static boolean isMutation(String action) { return !Set.of("join", "state", "heartbeat", "reconnect", "invite").contains(action); }
    private static byte[] inviteSigningKey() {
        String configured = System.getenv("AOO_INVITE_SIGNING_KEY");
        if (configured == null || configured.length() < 32) throw new IllegalStateException("AOO_INVITE_SIGNING_KEY must contain at least 32 characters");
        return configured.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
    private static AbsBaseRoom requireRoom(long roomId) { AbsBaseRoom room=RoomMgr.getInstance().getRoom(roomId); if(room==null)throw new IllegalArgumentException("room not found"); return room; }
    private static long parseRoomId(String value) { try { long id=Long.parseLong(value); if(id<=0)throw new NumberFormatException(); return id; } catch(NumberFormatException e){throw new IllegalArgumentException("numeric roomId required");} }
    private static void requireSeat(Map<String,Object> body, int actual) { if(body.get("seatId") instanceof Number n && n.intValue()!=actual)throw new SecurityException("seat mismatch"); }
    private static boolean bool(Map<String,Object> body,String key){if(!(body.get(key) instanceof Boolean b))throw new IllegalArgumentException(key+" must be boolean");return b;}
    private static int integer(Map<String,Object> body,String key,int min,int max){if(!(body.get(key) instanceof Number n))throw new IllegalArgumentException(key+" must be integer");int v=n.intValue();if(v<min||v>max)throw new IllegalArgumentException(key+" out of range");return v;}
    private static long positiveLong(Map<String,Object> body,String key){if(!(body.get(key) instanceof Number n))throw new IllegalArgumentException(key+" must be integer");long v=n.longValue();if(v<=0)throw new IllegalArgumentException(key+" out of range");return v;}
    private static String text(Map<String,Object> body,String key,int min,int max){if(!(body.get(key) instanceof String s)||s.length()<min||s.length()>max)throw new IllegalArgumentException(key+" has invalid length");return s;}
    private static <T> T throwInvalid(String message){throw new IllegalArgumentException(message);}
    private VoiceAsset authorizeVoice(long sender,long assetId,java.util.List<Long> members){try{String base=requiredEnv("AOO_MEDIA_INTERNAL_URL"),token=requiredEnv("AOO_MEDIA_ROOM_TOKEN");byte[] requestBody=mapper.writeValueAsBytes(Map.of("senderId",sender,"assetId",assetId,"memberIds",members));HttpRequest request=HttpRequest.newBuilder(URI.create(base).resolve("/internal/media/voice/authorize")).header("Authorization","Bearer "+token).header("Content-Type","application/json").POST(HttpRequest.BodyPublishers.ofByteArray(requestBody)).timeout(Duration.ofSeconds(3)).build();HttpResponse<byte[]> response=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build().send(request,HttpResponse.BodyHandlers.ofByteArray());if(response.statusCode()!=200)throw new SecurityException("voice asset authorization rejected");@SuppressWarnings("unchecked") Map<String,Object> root=mapper.readValue(response.body(),Map.class);@SuppressWarnings("unchecked") Map<String,Object> data=(Map<String,Object>)root.get("data");if(data==null||!"READY".equals(data.get("state")))throw new SecurityException("voice asset is not READY");return new VoiceAsset(((Number)data.get("durationMillis")).longValue(),String.valueOf(data.get("mimeType")));}catch(SecurityException e){throw e;}catch(Exception e){throw new IllegalStateException("MEDIA_AUTHORITY_UNAVAILABLE",e);}}
    private static String requiredEnv(String name){String value=System.getenv(name);if(value==null||value.isBlank())throw new IllegalStateException(name+" is required");return value;}
    private static Map<String,Object> success(SData_Result<?> result,Map<String,Object> payload){if(!ErrorCode.Success.equals(result.getCode()))throw new IllegalStateException("ROOM_COMMAND_REJECTED:"+result.getCode().value()+":"+result.getMsg());return payload;}
    private record Window(long startedAt,int count){}
    private record VoiceAsset(long durationMillis,String mimeType){}
}
