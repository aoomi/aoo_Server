package core.replay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.sql.DataSource;

/** Production JDBC read boundary for participant-scoped history and replay chunks. */
public final class RecordReplayQueryService {
    static final String TERMINAL_REPLAY_PREDICATE = "("
            + "JSON_EXTRACT(CAST(payload AS CHAR CHARACTER SET utf8mb4),'$.finished')=true "
            + "OR JSON_UNQUOTE(JSON_EXTRACT(CAST(payload AS CHAR CHARACTER SET utf8mb4),'$.phase')) IN ('FINISHED','ROUND_SETTLEMENT','SETTLED','DIRECT_WIN') "
            + "OR JSON_UNQUOTE(JSON_EXTRACT(CAST(payload AS CHAR CHARACTER SET utf8mb4),'$.tableSnapshot.phase')) IN ('FINISHED','ROUND_SETTLEMENT','SETTLED','DIRECT_WIN'))";
    public static final String API_VERSION = "1";
    static final String HISTORY_SQL = "SELECT p.room_id,p.last_set_id,p.played_at,s.settlement_version,s.result_payload FROM ("
            + "SELECT grants.room_id,MAX(grants.set_id) last_set_id,MAX(grants.granted_at) played_at FROM ("
            + "SELECT room_id,set_id,granted_at FROM replay_participant WHERE player_id=? UNION ALL "
            + "SELECT room_id,set_id,granted_at FROM replay_participant_archive WHERE player_id=?) grants "
            + "GROUP BY grants.room_id) p JOIN aoo_hall_room h ON h.room_id=p.room_id JOIN aoo_settlement s ON s.room_id=p.room_id "
            + "AND s.round_no=(SELECT MAX(last.round_no) FROM aoo_settlement last WHERE last.room_id=p.room_id) "
            + "WHERE (?=0 OR p.room_id<?) AND (?=0 OR p.played_at>=?) AND (?=0 OR p.played_at<?) "
            + "AND ((?=0 AND COALESCE(h.club_id,0)=0) OR (? > 0 AND h.club_id=?)) "
            + "ORDER BY p.room_id DESC LIMIT ?";
    static final String HISTORY_COUNT_SQL = "SELECT COUNT(*) FROM (SELECT grants.room_id,MAX(grants.granted_at) played_at FROM ("
            + "SELECT room_id,granted_at FROM replay_participant WHERE player_id=? UNION ALL "
            + "SELECT room_id,granted_at FROM replay_participant_archive WHERE player_id=?) grants "
            + "GROUP BY grants.room_id) p JOIN aoo_hall_room h ON h.room_id=p.room_id "
            + "WHERE EXISTS (SELECT 1 FROM aoo_settlement s WHERE s.room_id=p.room_id) "
            + "AND (?=0 OR p.played_at>=?) AND (?=0 OR p.played_at<?) "
            + "AND ((?=0 AND COALESCE(h.club_id,0)=0) OR (? > 0 AND h.club_id=?))";
    static final String BIG_WINNER_SQL = "SELECT p.room_id,s.result_payload FROM ("
            + "SELECT grants.room_id,MAX(grants.granted_at) played_at FROM ("
            + "SELECT room_id,granted_at FROM replay_participant WHERE player_id=? UNION ALL "
            + "SELECT room_id,granted_at FROM replay_participant_archive WHERE player_id=?) grants "
            + "GROUP BY grants.room_id) p JOIN aoo_hall_room h ON h.room_id=p.room_id "
            + "JOIN aoo_settlement s ON s.room_id=p.room_id "
            + "WHERE (?=0 OR p.played_at>=?) AND (?=0 OR p.played_at<?) "
            + "AND ((?=0 AND COALESCE(h.club_id,0)=0) OR (? > 0 AND h.club_id=?)) "
            + "ORDER BY p.room_id,s.round_no";
    private final DataSource source;
    private final ObjectMapper json;

    public RecordReplayQueryService(DataSource source, ObjectMapper json) {
        this.source = Objects.requireNonNull(source, "source");
        this.json = Objects.requireNonNull(json, "json");
    }

    public HistoryPage history(long playerId, long beforeRoomId, int limit) {
        return history(playerId, beforeRoomId, limit, 0, 0);
    }

    public HistoryPage history(long playerId, long beforeRoomId, int limit, long startAt, long endAt) {
        return history(playerId,beforeRoomId,limit,startAt,endAt,0);
    }

    public HistoryPage history(long playerId, long beforeRoomId, int limit, long startAt, long endAt, long clubId) {
        requirePlayer(playerId); requireLimit(limit, 100);
        if(clubId<0)throw new IllegalArgumentException("invalid history clubId");
        if (startAt < 0 || endAt < 0 || ((startAt == 0) != (endAt == 0)) || (startAt > 0 && endAt <= startAt))
            throw new IllegalArgumentException("invalid history date range");
        try (var c=source.getConnection();var q=c.prepareStatement(HISTORY_SQL)) {
            q.setLong(1,playerId);q.setLong(2,playerId);q.setLong(3,beforeRoomId);q.setLong(4,beforeRoomId);
            q.setLong(5,startAt);q.setTimestamp(6,startAt==0?new Timestamp(0):Timestamp.from(Instant.ofEpochMilli(startAt)));
            q.setLong(7,endAt);q.setTimestamp(8,endAt==0?new Timestamp(0):Timestamp.from(Instant.ofEpochMilli(endAt)));
            q.setLong(9,clubId);q.setLong(10,clubId);q.setLong(11,clubId);q.setInt(12,limit+1);
            List<HistoryItem> rows=new ArrayList<>();try(var r=q.executeQuery()){while(r.next())rows.add(new HistoryItem(
                    r.getLong(1),r.getInt(2),r.getTimestamp(3).toInstant(),r.getString(4),readJson(r.getString(5)))) ;}
            boolean more=rows.size()>limit;if(more)rows=new ArrayList<>(rows.subList(0,limit));
            long totalCount=countHistory(c,playerId,startAt,endAt,clubId);
            long bigWinnerCount=countBigWinner(c,playerId,startAt,endAt,clubId);
            System.out.println("[HallHistoryScope] playerId="+playerId+" clubId="+clubId+" itemCount="+rows.size()
                    +" totalCount="+totalCount+" bigWinnerCount="+bigWinnerCount+" startAt="+startAt+" endAt="+endAt);
            return new HistoryPage(List.copyOf(rows),rows.isEmpty()?beforeRoomId:rows.getLast().roomId(),more,totalCount,bigWinnerCount);
        } catch(Exception e){throw failure("list player history",e);}
    }

    /** Counts rooms where this player shares the highest positive match-total score. */
    private long countBigWinner(Connection c,long playerId,long startAt,long endAt,long clubId)throws Exception{
        Map<Long,Map<String,Long>> totals=new HashMap<>();
        try(var q=c.prepareStatement(BIG_WINNER_SQL)){
            q.setLong(1,playerId);q.setLong(2,playerId);
            q.setLong(3,startAt);q.setTimestamp(4,startAt==0?new Timestamp(0):Timestamp.from(Instant.ofEpochMilli(startAt)));
            q.setLong(5,endAt);q.setTimestamp(6,endAt==0?new Timestamp(0):Timestamp.from(Instant.ofEpochMilli(endAt)));
            q.setLong(7,clubId);q.setLong(8,clubId);q.setLong(9,clubId);
            try(var r=q.executeQuery()){while(r.next()){
                JsonNode entries=readJson(r.getString(2));
                entries=entries==null?null:entries.path("entries");
                if(entries==null||!entries.isArray())continue;
                Map<String,Long> roomTotals=totals.computeIfAbsent(r.getLong(1),ignored->new HashMap<>());
                for(JsonNode entry:entries){
                    String entryPlayerId=entry.path("playerId").asText("");
                    if(entryPlayerId.isBlank())continue;
                    roomTotals.merge(entryPlayerId,entry.path("scoreDelta").asLong(0),Long::sum);
                }
            }}
        }
        String target=Long.toString(playerId);long winners=0;
        for(Map<String,Long> roomTotals:totals.values()){
            long maximum=roomTotals.values().stream().mapToLong(Long::longValue).max().orElse(0);
            if(maximum>0&&roomTotals.getOrDefault(target,Long.MIN_VALUE)==maximum)winners++;
        }
        return winners;
    }

    private long countHistory(Connection c,long playerId,long startAt,long endAt,long clubId)throws Exception{
        try(var q=c.prepareStatement(HISTORY_COUNT_SQL)){
            q.setLong(1,playerId);q.setLong(2,playerId);
            q.setLong(3,startAt);q.setTimestamp(4,startAt==0?new Timestamp(0):Timestamp.from(Instant.ofEpochMilli(startAt)));
            q.setLong(5,endAt);q.setTimestamp(6,endAt==0?new Timestamp(0):Timestamp.from(Instant.ofEpochMilli(endAt)));
            q.setLong(7,clubId);q.setLong(8,clubId);q.setLong(9,clubId);
            try(var r=q.executeQuery()){return r.next()?r.getLong(1):0;}
        }
    }

    public HistoryDetail detail(long playerId,long roomId) {
        requirePlayer(playerId);if(roomId<=0)throw new IllegalArgumentException("invalid roomId");
        try(var c=source.getConnection()){
            authorize(c,playerId,roomId,null);
            String sql="SELECT s.round_no,s.settlement_version,s.result_payload,s.created_at,c.short_code FROM aoo_settlement s LEFT JOIN replay_short_code c ON c.room_id=s.room_id AND c.set_id=s.round_no-1 AND c.status='ACTIVE' WHERE s.room_id=? ORDER BY s.round_no";
            try(var q=c.prepareStatement(sql)){q.setLong(1,roomId);List<RoundResult> rounds=new ArrayList<>();try(var r=q.executeQuery()){
                while(r.next()){
                    int roundNo=r.getInt(1);
                    JsonNode settlement=enrichSettlement(c,roomId,roundNo,readJson(r.getString(3)));
                    // A replay-backed settlement without a terminal snapshot was produced
                    // before the round ended.  Never expose it as a pageable completed round.
                    if(settlement==null)continue;
                    rounds.add(new RoundResult(roundNo,r.getString(2),settlement,r.getTimestamp(4).toInstant(),r.getString(5)));
                }
                RuleContext ruleContext=ruleContext(c,roomId);
                System.out.println("[RecordSettlementDetail] roomId="+roomId+" playerId="+playerId
                        +" gameCode="+ruleContext.gameCode()+" playFamily="+ruleContext.playFamily()
                        +" smallSettleTemplate="+ruleContext.smallSettleTemplate()+" roundCount="+rounds.size());
                return new HistoryDetail(roomId,ruleContext.gameCode(),ruleContext.playFamily(),
                        ruleContext.smallSettleTemplate(),List.copyOf(rounds),ruleContext.snapshot(),ruleContext.fields());}}
        }catch(Exception e){if(e instanceof SecurityException security)throw security;throw failure("read history detail",e);}
    }

    public ReplayChunk replay(long playerId,long roomId,int setId,long afterSequence,int limit){
        requirePlayer(playerId);if(roomId<=0||setId<0||afterSequence<0)throw new IllegalArgumentException("invalid replay cursor");requireLimit(limit,500);
        try(var c=source.getConnection()){
            authorize(c,playerId,roomId,setId);
            long perspectivePlayerId=replayPerspective(c,playerId,roomId,setId);
            String sql="SELECT event_sequence,message_id,schema_version,play_version,payload,content_hash FROM ("
                    +"SELECT event_sequence,message_id,schema_version,play_version,payload,content_hash FROM ("
                    +"SELECT e.*,ROW_NUMBER() OVER(PARTITION BY event_sequence ORDER BY (owner_player_id=?) DESC,owner_player_id) preferred FROM ("
                    +"SELECT event_sequence,message_id,schema_version,play_version,payload,content_hash,owner_player_id FROM perspective_replay_event WHERE room_id=? AND set_id=? AND visibility='PLAYER_PRIVATE' UNION ALL "
                    +"SELECT event_sequence,message_id,schema_version,play_version,payload,content_hash,owner_player_id FROM perspective_replay_event_archive WHERE room_id=? AND set_id=? AND visibility='PLAYER_PRIVATE') e) ranked WHERE preferred=1 "
                    +"UNION ALL SELECT event_sequence,message_id,schema_version,play_version,payload,content_hash FROM perspective_replay_event WHERE room_id=? AND set_id=? AND visibility='PUBLIC' AND owner_player_id=0 "
                    +"UNION ALL SELECT event_sequence,message_id,schema_version,play_version,payload,content_hash FROM perspective_replay_event_archive WHERE room_id=? AND set_id=? AND visibility='PUBLIC' AND owner_player_id=0) selected "
                    +"WHERE event_sequence>? ORDER BY event_sequence LIMIT ?";
            try(var q=c.prepareStatement(sql)){q.setLong(1,perspectivePlayerId);q.setLong(2,roomId);q.setInt(3,setId);q.setLong(4,roomId);q.setInt(5,setId);q.setLong(6,roomId);q.setInt(7,setId);q.setLong(8,roomId);q.setInt(9,setId);q.setLong(10,afterSequence);q.setInt(11,limit+1);
                List<ReplayEvent> events=new ArrayList<>();try(var r=q.executeQuery()){while(r.next())events.add(new ReplayEvent(r.getLong(1),r.getString(2),r.getInt(3),r.getString(4),r.getBytes(5),r.getString(6)));}
                boolean more=events.size()>limit;if(more)events=new ArrayList<>(events.subList(0,limit));long next=events.isEmpty()?afterSequence:events.getLast().sequence();
                System.out.println("[ReplayRead] roomId="+roomId+" playerId="+playerId+" perspectivePlayerId="+perspectivePlayerId+" setId="+setId+" eventCount="+events.size()+" afterSequence="+afterSequence);
                return new ReplayChunk(roomId,setId,List.copyOf(events),next,more,chunkHash(events));}
        }catch(Exception e){if(e instanceof SecurityException security)throw security;throw failure("read replay chunk",e);}
    }

    private void authorize(Connection c,long playerId,long roomId,Integer setId)throws Exception{
        String sql="SELECT 1 FROM (SELECT room_id,set_id,player_id FROM replay_participant UNION ALL SELECT room_id,set_id,player_id FROM replay_participant_archive UNION ALL SELECT c.room_id,c.set_id,a.player_id FROM replay_short_code_access a JOIN replay_short_code c ON c.short_code=a.short_code) p WHERE player_id=? AND room_id=?"+(setId==null?"":" AND set_id=?")+" LIMIT 1";
        try(var q=c.prepareStatement(sql)){q.setLong(1,playerId);q.setLong(2,roomId);if(setId!=null)q.setInt(3,setId);try(var r=q.executeQuery()){if(!r.next())throw new SecurityException("record access denied");}}
    }

    private long replayPerspective(Connection c,long playerId,long roomId,int setId)throws Exception{
        String participantSql="SELECT 1 FROM ("
                +"SELECT room_id,set_id,player_id FROM replay_participant UNION ALL "
                +"SELECT room_id,set_id,player_id FROM replay_participant_archive) p "
                +"WHERE room_id=? AND set_id=? AND player_id=? LIMIT 1";
        try(var q=c.prepareStatement(participantSql)){
            q.setLong(1,roomId);q.setInt(2,setId);q.setLong(3,playerId);
            try(var r=q.executeQuery()){if(r.next())return playerId;}
        }
        String sharedSql="SELECT p.player_id FROM ("
                +"SELECT player_id,seat_id FROM replay_participant WHERE room_id=? AND set_id=? UNION ALL "
                +"SELECT player_id,seat_id FROM replay_participant_archive WHERE room_id=? AND set_id=?) p "
                +"JOIN replay_short_code c ON c.room_id=? AND c.set_id=? AND c.status='ACTIVE' "
                +"JOIN replay_short_code_access a ON a.short_code=c.short_code AND a.player_id=? "
                +"ORDER BY p.seat_id,p.player_id LIMIT 1";
        try(var q=c.prepareStatement(sharedSql)){
            q.setLong(1,roomId);q.setInt(2,setId);q.setLong(3,roomId);q.setInt(4,setId);
            q.setLong(5,roomId);q.setInt(6,setId);q.setLong(7,playerId);
            try(var r=q.executeQuery()){if(r.next())return r.getLong(1);}
        }
        throw new SecurityException("record access denied");
    }
    private RuleContext ruleContext(Connection c,long roomId)throws Exception{
        String sql="SELECT h.rules_json,i.ui_schema,g.game_code,g.family_code FROM aoo_hall_room h "
                +"JOIN aoo_game_catalog g ON g.game_id=h.game_id "
                +"LEFT JOIN aoo_compiled_room_create_index i ON i.game_id=h.game_id AND i.region_code=h.classification_region_code "
                +"AND i.play_version=h.play_version AND i.release_id=h.release_id AND i.index_generation=h.index_generation "
                +"WHERE h.room_id=? LIMIT 1";
        try(var q=c.prepareStatement(sql)){q.setLong(1,roomId);try(var r=q.executeQuery()){
            if(!r.next())throw new IllegalStateException("history room catalog context missing: "+roomId);
            JsonNode snapshot=readJson(r.getString(1));JsonNode ui=readJson(r.getString(2));
            return new RuleContext(snapshot,ui==null?null:ui.path("fields"),r.getString(3),r.getString(4),
                    ui==null?"":ui.path("smallSettleTemplate").asText(""));}}
    }
    private JsonNode enrichSettlement(Connection c,long roomId,int roundNo,JsonNode raw)throws Exception{
        if(!(raw instanceof ObjectNode settlement))return raw;
        // Different authoritative families expose terminal state either as a root flag,
        // a root phase, or the table snapshot phase.  History must recognize all three;
        // otherwise a valid round is returned without its cards even though replay data exists.
        String sql="SELECT payload FROM (SELECT event_sequence,payload FROM perspective_replay_event WHERE room_id=? AND set_id=? AND visibility='PLAYER_PRIVATE' UNION ALL SELECT event_sequence,payload FROM perspective_replay_event_archive WHERE room_id=? AND set_id=? AND visibility='PLAYER_PRIVATE') e WHERE "
                +TERMINAL_REPLAY_PREDICATE+" ORDER BY event_sequence DESC LIMIT 1";
        JsonNode snapshot=null;
        try(var q=c.prepareStatement(sql)){int setId=Math.max(0,roundNo-1);q.setLong(1,roomId);q.setInt(2,setId);q.setLong(3,roomId);q.setInt(4,setId);try(var r=q.executeQuery()){if(r.next())snapshot=json.readTree(r.getBytes(1));}}
        if(snapshot==null||!snapshot.isObject())return hasPrivateReplay(c,roomId,roundNo)?null:settlement;
        JsonNode seats=snapshot.path("seats");
        JsonNode sourceEntries=settlement.path("entries");
        if(!sourceEntries.isArray()||!seats.isObject())return settlement;
        ArrayNode enriched=json.createArrayNode();
        for(JsonNode source:sourceEntries){
            ObjectNode entry=source.deepCopy();String playerId=source.path("playerId").asText();
            JsonNode seat=null;String seatId="";var fields=seats.fields();while(fields.hasNext()){var field=fields.next();JsonNode candidate=field.getValue();if(playerId.equals(candidate.path("playerId").asText())){seat=candidate;seatId=field.getKey();break;}}
            if(seat!=null){for(String key:List.of("name","headImageUrl","remainingCards","playedCards","cardCount","roundScore","initialPatterns")){JsonNode value=seat.get(key);if(value!=null)entry.set(key,value);}}
            ArrayNode hands=json.createArrayNode();JsonNode history=snapshot.path("playHistory");if(history.isArray())for(JsonNode hand:history)if(seatId.equals(hand.path("seat").asText()))hands.add(hand);
            entry.set("playedHands",hands);
            enriched.add(entry);
        }
        ObjectNode result=settlement.deepCopy();result.set("entries",enriched);JsonNode history=snapshot.get("playHistory");if(history!=null)result.set("playHistory",history);return result;
    }
    private boolean hasPrivateReplay(Connection c,long roomId,int roundNo)throws Exception{
        String sql="SELECT 1 FROM (SELECT room_id,set_id FROM perspective_replay_event WHERE room_id=? AND set_id=? AND visibility='PLAYER_PRIVATE' UNION ALL SELECT room_id,set_id FROM perspective_replay_event_archive WHERE room_id=? AND set_id=? AND visibility='PLAYER_PRIVATE') e LIMIT 1";
        int setId=Math.max(0,roundNo-1);
        try(var q=c.prepareStatement(sql)){q.setLong(1,roomId);q.setInt(2,setId);q.setLong(3,roomId);q.setInt(4,setId);try(var r=q.executeQuery()){return r.next();}}
    }
    private JsonNode readJson(String value)throws Exception{return value==null?null:json.readTree(value);}
    private static String chunkHash(List<ReplayEvent> events)throws Exception{MessageDigest d=MessageDigest.getInstance("SHA-256");for(var e:events){d.update(Long.toString(e.sequence()).getBytes(StandardCharsets.UTF_8));d.update(e.contentHash().getBytes(StandardCharsets.US_ASCII));}return HexFormat.of().formatHex(d.digest());}
    private static void requirePlayer(long id){if(id<=0)throw new SecurityException("authenticated player required");}
    private static void requireLimit(int limit,int max){if(limit<1||limit>max)throw new IllegalArgumentException("invalid limit");}
    private static IllegalStateException failure(String action,Exception e){return new IllegalStateException("cannot "+action,e);}
    public record HistoryItem(long roomId,int lastSetId,Instant playedAt,String playVersion,JsonNode settlement){}
    public record HistoryPage(List<HistoryItem> items,long nextBeforeRoomId,boolean hasMore,long totalCount,long bigWinnerCount){
        public HistoryPage(List<HistoryItem> items,long nextBeforeRoomId,boolean hasMore){
            this(items,nextBeforeRoomId,hasMore,items==null?0:items.size(),0);
        }
        public HistoryPage(List<HistoryItem> items,long nextBeforeRoomId,boolean hasMore,long totalCount){
            this(items,nextBeforeRoomId,hasMore,totalCount,0);
        }
    }
    public record RoundResult(int roundNo,String playVersion,JsonNode settlement,Instant settledAt,String replayCode){}
    public record HistoryDetail(long roomId,String gameCode,String playFamily,String smallSettleTemplate,
                                List<RoundResult> rounds,JsonNode ruleSnapshot,JsonNode ruleFields){}
    private record RuleContext(JsonNode snapshot,JsonNode fields,String gameCode,String playFamily,String smallSettleTemplate){}
    public record ReplayEvent(long sequence,String messageId,int schemaVersion,String playVersion,byte[] payload,String contentHash){public ReplayEvent{payload=payload.clone();}public byte[] payload(){return payload.clone();}}
    public record ReplayChunk(long roomId,int setId,List<ReplayEvent> events,long nextSequence,boolean hasMore,String contentHash){}
}
