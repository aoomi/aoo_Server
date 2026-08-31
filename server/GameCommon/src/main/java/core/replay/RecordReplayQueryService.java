package core.replay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
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
        requirePlayer(playerId); requireLimit(limit, 100);
        String sql = "SELECT p.room_id,p.last_set_id,p.played_at,s.settlement_version,s.result_payload FROM ("
                + "SELECT grants.room_id,MAX(grants.set_id) last_set_id,MAX(grants.granted_at) played_at FROM ("
                + "SELECT room_id,set_id,granted_at FROM replay_participant WHERE player_id=? UNION ALL "
                + "SELECT room_id,set_id,granted_at FROM replay_participant_archive WHERE player_id=?) grants "
                + "GROUP BY grants.room_id) p LEFT JOIN aoo_settlement s ON s.room_id=p.room_id "
                + "AND s.round_no=(SELECT MAX(last.round_no) FROM aoo_settlement last WHERE last.room_id=p.room_id) "
                + "WHERE (?=0 OR p.room_id<?) ORDER BY p.room_id DESC LIMIT ?";
        try (var c=source.getConnection();var q=c.prepareStatement(sql)) {
            q.setLong(1,playerId);q.setLong(2,playerId);q.setLong(3,beforeRoomId);q.setLong(4,beforeRoomId);q.setInt(5,limit+1);
            List<HistoryItem> rows=new ArrayList<>();try(var r=q.executeQuery()){while(r.next())rows.add(new HistoryItem(
                    r.getLong(1),r.getInt(2),r.getTimestamp(3).toInstant(),r.getString(4),readJson(r.getString(5)))) ;}
            boolean more=rows.size()>limit;if(more)rows=new ArrayList<>(rows.subList(0,limit));
            return new HistoryPage(List.copyOf(rows),rows.isEmpty()?beforeRoomId:rows.getLast().roomId(),more);
        } catch(Exception e){throw failure("list player history",e);}
    }

    public HistoryDetail detail(long playerId,long roomId) {
        requirePlayer(playerId);if(roomId<=0)throw new IllegalArgumentException("invalid roomId");
        try(var c=source.getConnection()){
            authorize(c,playerId,roomId,null);
            String sql="SELECT round_no,settlement_version,result_payload,created_at FROM aoo_settlement WHERE room_id=? ORDER BY round_no";
            try(var q=c.prepareStatement(sql)){q.setLong(1,roomId);List<RoundResult> rounds=new ArrayList<>();try(var r=q.executeQuery()){
                while(r.next())rounds.add(new RoundResult(r.getInt(1),r.getString(2),readJson(r.getString(3)),r.getTimestamp(4).toInstant()));}
                return new HistoryDetail(roomId,List.copyOf(rounds));}
        }catch(Exception e){if(e instanceof SecurityException security)throw security;throw failure("read history detail",e);}
    }

    public ReplayChunk replay(long playerId,long roomId,int setId,long afterSequence,int limit){
        requirePlayer(playerId);if(roomId<=0||setId<0||afterSequence<0)throw new IllegalArgumentException("invalid replay cursor");requireLimit(limit,500);
        try(var c=source.getConnection()){
            authorize(c,playerId,roomId,setId);
            String sql="SELECT event_sequence,message_id,schema_version,play_version,payload,content_hash FROM ("
                    +"SELECT event_sequence,message_id,schema_version,play_version,payload,content_hash,visibility,owner_player_id FROM perspective_replay_event WHERE room_id=? AND set_id=? UNION ALL "
                    +"SELECT event_sequence,message_id,schema_version,play_version,payload,content_hash,visibility,owner_player_id FROM perspective_replay_event_archive WHERE room_id=? AND set_id=?) e "
                    +"WHERE event_sequence>? AND ((visibility='PUBLIC' AND owner_player_id=0) OR (visibility='PLAYER_PRIVATE' AND owner_player_id=?)) ORDER BY event_sequence LIMIT ?";
            try(var q=c.prepareStatement(sql)){q.setLong(1,roomId);q.setInt(2,setId);q.setLong(3,roomId);q.setInt(4,setId);q.setLong(5,afterSequence);q.setLong(6,playerId);q.setInt(7,limit+1);
                List<ReplayEvent> events=new ArrayList<>();try(var r=q.executeQuery()){while(r.next())events.add(new ReplayEvent(r.getLong(1),r.getString(2),r.getInt(3),r.getString(4),r.getBytes(5),r.getString(6)));}
                boolean more=events.size()>limit;if(more)events=new ArrayList<>(events.subList(0,limit));long next=events.isEmpty()?afterSequence:events.getLast().sequence();
                return new ReplayChunk(roomId,setId,List.copyOf(events),next,more,chunkHash(events));}
        }catch(Exception e){if(e instanceof SecurityException security)throw security;throw failure("read replay chunk",e);}
    }

    private void authorize(Connection c,long playerId,long roomId,Integer setId)throws Exception{
        String sql="SELECT 1 FROM (SELECT room_id,set_id,player_id FROM replay_participant UNION ALL SELECT room_id,set_id,player_id FROM replay_participant_archive) p WHERE player_id=? AND room_id=?"+(setId==null?"":" AND set_id=?")+" LIMIT 1";
        try(var q=c.prepareStatement(sql)){q.setLong(1,playerId);q.setLong(2,roomId);if(setId!=null)q.setInt(3,setId);try(var r=q.executeQuery()){if(!r.next())throw new SecurityException("record access denied");}}
    }
    private JsonNode readJson(String value)throws Exception{return value==null?null:json.readTree(value);}
    private static String chunkHash(List<ReplayEvent> events)throws Exception{MessageDigest d=MessageDigest.getInstance("SHA-256");for(var e:events){d.update(Long.toString(e.sequence()).getBytes(StandardCharsets.UTF_8));d.update(e.contentHash().getBytes(StandardCharsets.US_ASCII));}return HexFormat.of().formatHex(d.digest());}
    private static void requirePlayer(long id){if(id<=0)throw new SecurityException("authenticated player required");}
    private static void requireLimit(int limit,int max){if(limit<1||limit>max)throw new IllegalArgumentException("invalid limit");}
    private static IllegalStateException failure(String action,Exception e){return new IllegalStateException("cannot "+action,e);}
    public record HistoryItem(long roomId,int lastSetId,Instant playedAt,String playVersion,JsonNode settlement){}
    public record HistoryPage(List<HistoryItem> items,long nextBeforeRoomId,boolean hasMore){}
    public record RoundResult(int roundNo,String playVersion,JsonNode settlement,Instant settledAt){}
    public record HistoryDetail(long roomId,List<RoundResult> rounds){}
    public record ReplayEvent(long sequence,String messageId,int schemaVersion,String playVersion,byte[] payload,String contentHash){public ReplayEvent{payload=payload.clone();}public byte[] payload(){return payload.clone();}}
    public record ReplayChunk(long roomId,int setId,List<ReplayEvent> events,long nextSequence,boolean hasMore,String contentHash){}
}
