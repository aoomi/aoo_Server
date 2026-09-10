package core.replay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import javax.sql.DataSource;

/** Production JDBC read boundary for participant-scoped history and replay chunks. */
public final class RecordReplayQueryService {
    public static final String API_VERSION = "1";
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
        requirePlayer(playerId); requireLimit(limit, 100);
        if (startAt < 0 || endAt < 0 || ((startAt == 0) != (endAt == 0)) || (startAt > 0 && endAt <= startAt))
            throw new IllegalArgumentException("invalid history date range");
        String sql = "SELECT p.room_id,p.last_set_id,p.played_at,s.settlement_version,s.result_payload FROM ("
                + "SELECT grants.room_id,MAX(grants.set_id) last_set_id,MAX(grants.granted_at) played_at FROM ("
                + "SELECT room_id,set_id,granted_at FROM replay_participant WHERE player_id=? UNION ALL "
                + "SELECT room_id,set_id,granted_at FROM replay_participant_archive WHERE player_id=?) grants "
                + "GROUP BY grants.room_id) p LEFT JOIN aoo_settlement s ON s.room_id=p.room_id "
                + "AND s.round_no=(SELECT MAX(last.round_no) FROM aoo_settlement last WHERE last.room_id=p.room_id) "
                + "WHERE (?=0 OR p.room_id<?) AND (?=0 OR p.played_at>=?) AND (?=0 OR p.played_at<?) "
                + "ORDER BY p.room_id DESC LIMIT ?";
        try (var c=source.getConnection();var q=c.prepareStatement(sql)) {
            q.setLong(1,playerId);q.setLong(2,playerId);q.setLong(3,beforeRoomId);q.setLong(4,beforeRoomId);
            q.setLong(5,startAt);q.setTimestamp(6,startAt==0?new Timestamp(0):Timestamp.from(Instant.ofEpochMilli(startAt)));
            q.setLong(7,endAt);q.setTimestamp(8,endAt==0?new Timestamp(0):Timestamp.from(Instant.ofEpochMilli(endAt)));
            q.setInt(9,limit+1);
            List<HistoryItem> rows=new ArrayList<>();try(var r=q.executeQuery()){while(r.next())rows.add(new HistoryItem(
                    r.getLong(1),r.getInt(2),r.getTimestamp(3).toInstant(),r.getString(4),readJson(r.getString(5)))) ;}
            boolean more=rows.size()>limit;if(more)rows=new ArrayList<>(rows.subList(0,limit));
            long totalCount=countHistory(c,playerId,startAt,endAt);
            return new HistoryPage(List.copyOf(rows),rows.isEmpty()?beforeRoomId:rows.getLast().roomId(),more,totalCount);
        } catch(Exception e){throw failure("list player history",e);}
    }

    private long countHistory(Connection c,long playerId,long startAt,long endAt)throws Exception{
        String sql="SELECT COUNT(*) FROM (SELECT grants.room_id,MAX(grants.granted_at) played_at FROM ("
                +"SELECT room_id,granted_at FROM replay_participant WHERE player_id=? UNION ALL "
                +"SELECT room_id,granted_at FROM replay_participant_archive WHERE player_id=?) grants "
                +"GROUP BY grants.room_id) p WHERE (?=0 OR p.played_at>=?) AND (?=0 OR p.played_at<?)";
        try(var q=c.prepareStatement(sql)){
            q.setLong(1,playerId);q.setLong(2,playerId);
            q.setLong(3,startAt);q.setTimestamp(4,startAt==0?new Timestamp(0):Timestamp.from(Instant.ofEpochMilli(startAt)));
            q.setLong(5,endAt);q.setTimestamp(6,endAt==0?new Timestamp(0):Timestamp.from(Instant.ofEpochMilli(endAt)));
            try(var r=q.executeQuery()){return r.next()?r.getLong(1):0;}
        }
    }

    public HistoryDetail detail(long playerId,long roomId) {
        requirePlayer(playerId);if(roomId<=0)throw new IllegalArgumentException("invalid roomId");
        try(var c=source.getConnection()){
            authorize(c,playerId,roomId,null);
            String sql="SELECT s.round_no,s.settlement_version,s.result_payload,s.created_at,c.short_code FROM aoo_settlement s LEFT JOIN replay_short_code c ON c.room_id=s.room_id AND c.set_id=s.round_no-1 AND c.status='ACTIVE' WHERE s.room_id=? ORDER BY s.round_no";
            try(var q=c.prepareStatement(sql)){q.setLong(1,roomId);List<RoundResult> rounds=new ArrayList<>();try(var r=q.executeQuery()){
                while(r.next())rounds.add(new RoundResult(r.getInt(1),r.getString(2),readJson(r.getString(3)),r.getTimestamp(4).toInstant(),r.getString(5)));}
                RuleContext ruleContext=ruleContext(c,roomId);
                return new HistoryDetail(roomId,List.copyOf(rounds),ruleContext.snapshot(),ruleContext.fields());}
        }catch(Exception e){if(e instanceof SecurityException security)throw security;throw failure("read history detail",e);}
    }

    public ReplayChunk replay(long playerId,long roomId,int setId,long afterSequence,int limit){
        requirePlayer(playerId);if(roomId<=0||setId<0||afterSequence<0)throw new IllegalArgumentException("invalid replay cursor");requireLimit(limit,500);
        try(var c=source.getConnection()){
            authorize(c,playerId,roomId,setId);
            long perspectivePlayerId=replayPerspective(c,playerId,roomId,setId);
            String sql="SELECT event_sequence,message_id,schema_version,play_version,payload,content_hash FROM ("
                    +"SELECT event_sequence,message_id,schema_version,play_version,payload,content_hash,visibility,owner_player_id FROM perspective_replay_event WHERE room_id=? AND set_id=? UNION ALL "
                    +"SELECT event_sequence,message_id,schema_version,play_version,payload,content_hash,visibility,owner_player_id FROM perspective_replay_event_archive WHERE room_id=? AND set_id=?) e "
                    +"WHERE event_sequence>? AND ((visibility='PUBLIC' AND owner_player_id=0) OR (visibility='PLAYER_PRIVATE' AND owner_player_id=?)) ORDER BY event_sequence LIMIT ?";
            try(var q=c.prepareStatement(sql)){q.setLong(1,roomId);q.setInt(2,setId);q.setLong(3,roomId);q.setInt(4,setId);q.setLong(5,afterSequence);q.setLong(6,perspectivePlayerId);q.setInt(7,limit+1);
                List<ReplayEvent> events=new ArrayList<>();try(var r=q.executeQuery()){while(r.next())events.add(new ReplayEvent(r.getLong(1),r.getString(2),r.getInt(3),r.getString(4),r.getBytes(5),r.getString(6)));}
                boolean more=events.size()>limit;if(more)events=new ArrayList<>(events.subList(0,limit));long next=events.isEmpty()?afterSequence:events.getLast().sequence();
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
        String sql="SELECT h.rules_json,i.ui_schema FROM aoo_hall_room h LEFT JOIN aoo_compiled_room_create_index i ON i.game_id=h.game_id AND i.region_code=h.classification_region_code AND i.play_version=h.play_version AND i.release_id=h.release_id AND i.index_generation=h.index_generation WHERE h.room_id=? LIMIT 1";
        try(var q=c.prepareStatement(sql)){q.setLong(1,roomId);try(var r=q.executeQuery()){if(!r.next())return new RuleContext(null,null);JsonNode snapshot=readJson(r.getString(1));JsonNode ui=readJson(r.getString(2));return new RuleContext(snapshot,ui==null?null:ui.path("fields"));}}
    }
    private JsonNode readJson(String value)throws Exception{return value==null?null:json.readTree(value);}
    private static String chunkHash(List<ReplayEvent> events)throws Exception{MessageDigest d=MessageDigest.getInstance("SHA-256");for(var e:events){d.update(Long.toString(e.sequence()).getBytes(StandardCharsets.UTF_8));d.update(e.contentHash().getBytes(StandardCharsets.US_ASCII));}return HexFormat.of().formatHex(d.digest());}
    private static void requirePlayer(long id){if(id<=0)throw new SecurityException("authenticated player required");}
    private static void requireLimit(int limit,int max){if(limit<1||limit>max)throw new IllegalArgumentException("invalid limit");}
    private static IllegalStateException failure(String action,Exception e){return new IllegalStateException("cannot "+action,e);}
    public record HistoryItem(long roomId,int lastSetId,Instant playedAt,String playVersion,JsonNode settlement){}
    public record HistoryPage(List<HistoryItem> items,long nextBeforeRoomId,boolean hasMore,long totalCount){
        public HistoryPage(List<HistoryItem> items,long nextBeforeRoomId,boolean hasMore){
            this(items,nextBeforeRoomId,hasMore,items==null?0:items.size());
        }
    }
    public record RoundResult(int roundNo,String playVersion,JsonNode settlement,Instant settledAt,String replayCode){}
    public record HistoryDetail(long roomId,List<RoundResult> rounds,JsonNode ruleSnapshot,JsonNode ruleFields){}
    private record RuleContext(JsonNode snapshot,JsonNode fields){}
    public record ReplayEvent(long sequence,String messageId,int schemaVersion,String playVersion,byte[] payload,String contentHash){public ReplayEvent{payload=payload.clone();}public byte[] payload(){return payload.clone();}}
    public record ReplayChunk(long roomId,int setId,List<ReplayEvent> events,long nextSequence,boolean hasMore,String contentHash){}
}
