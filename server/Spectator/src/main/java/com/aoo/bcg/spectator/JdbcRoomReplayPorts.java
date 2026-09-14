package com.aoo.bcg.spectator;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.sql.DataSource;
import java.time.Instant;
import java.util.*;

/** JDBC adapters over the canonical Hall room and perspective replay/event stores. */
public final class JdbcRoomReplayPorts implements SpectatorPorts.RoomPort,SpectatorPorts.ReplayPort {
 private final DataSource source;private final ObjectMapper json;
 public JdbcRoomReplayPorts(DataSource source,ObjectMapper json){this.source=Objects.requireNonNull(source);this.json=Objects.requireNonNull(json);}
 public SpectatorPorts.RoomSummary summary(long roomId){String sql="SELECT r.room_id,r.game_id,r.classification_region_code,r.play_version,r.state,COUNT(m.account_id) FROM aoo_hall_room r LEFT JOIN aoo_hall_room_member m ON m.room_id=r.room_id AND m.status='JOINED' WHERE r.room_id=? GROUP BY r.room_id,r.game_id,r.classification_region_code,r.play_version,r.state";try(var c=source.getConnection();var q=c.prepareStatement(sql)){q.setLong(1,roomId);try(var rs=q.executeQuery()){if(!rs.next())throw new NoSuchElementException("room not found");return new SpectatorPorts.RoomSummary(rs.getLong(1),rs.getLong(2),rs.getString(3),rs.getString(4),rs.getString(5),rs.getInt(6));}}catch(NoSuchElementException e){throw e;}catch(Exception e){throw new IllegalStateException("room port failed",e);}}
 public boolean mayApprove(long roomId,long playerId){try(var c=source.getConnection();var q=c.prepareStatement("SELECT 1 FROM aoo_hall_room WHERE room_id=? AND owner_account_id=? AND state IN('OPEN','PLAYING')")){q.setLong(1,roomId);q.setLong(2,playerId);try(var r=q.executeQuery()){return r.next();}}catch(Exception e){throw new IllegalStateException("room authority lookup failed",e);}}
 public List<SpectatorPorts.Event> publicEvents(long roomId,long after,Instant visibleBefore,int limit){String sql="SELECT event_sequence,event_type,event_payload,created_at FROM aoo_room_event WHERE room_id=? AND event_sequence>? AND visibility='PUBLIC' AND owner_player_id=0 AND created_at<=? ORDER BY event_sequence LIMIT ?";try(var c=source.getConnection();var q=c.prepareStatement(sql)){q.setLong(1,roomId);q.setLong(2,after);q.setTimestamp(3,java.sql.Timestamp.from(visibleBefore));q.setInt(4,Math.max(1,Math.min(limit,500)));try(var rs=q.executeQuery()){var out=new ArrayList<SpectatorPorts.Event>();while(rs.next())out.add(new SpectatorPorts.Event(rs.getLong(1),rs.getString(2),json.readValue(rs.getString(3),Object.class),rs.getTimestamp(4).toInstant()));return List.copyOf(out);}}catch(Exception e){throw new IllegalStateException("replay port failed",e);}}
}
