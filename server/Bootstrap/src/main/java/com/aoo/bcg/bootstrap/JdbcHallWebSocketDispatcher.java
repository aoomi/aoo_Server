package com.aoo.bcg.bootstrap;

import com.aoo.bcg.gateway.ConnectionIdentity;
import com.aoo.bcg.gateway.GatewayWebSocketFrameHandler;
import com.aoo.bcg.gateway.WebSocketFrame;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/** Authoritative Hall WSS session adapter. It never accepts client-supplied player identity. */
final class JdbcHallWebSocketDispatcher implements GatewayWebSocketFrameHandler.NonRoomDispatcher {
    private final DataSource source;
    JdbcHallWebSocketDispatcher(DataSource source) { this.source=Objects.requireNonNull(source); }
    @Override public Map<String,Object> dispatch(ConnectionIdentity identity, WebSocketFrame frame) {
        String action=String.valueOf(frame.body().getOrDefault("action",""));
        if ("account.session_dispatch".equals(frame.msgId())) return switch(action) {
            case "base.C1004Login" -> Map.of("isNeedCreateRole",false);
            case "base.C1001CreateRole", "base.C1006RoleLogin", "base.C1008Login" -> profile(identity.userId());
            default -> throw new IllegalArgumentException("unsupported account session action");
        };
        if ("hall.dispatch".equals(frame.msgId()) && ("game.C1101GetRoomID".equals(action)
                || "room.CBaseRoomConfig".equals(action))) return currentRoom(identity.userId());
        if ("club.dispatch".equals(frame.msgId()) && "club.CClubInvited".equals(action))
            return Map.of("items",clubInvitations(identity.userId()));
        if ("club.dispatch".equals(frame.msgId()) && "union.CUnionNotify".equals(action))
            return Map.of("items",unionNotifications(identity.userId()));
        throw new IllegalArgumentException("unsupported hall action");
    }
    private Map<String,Object> currentRoom(long accountId) {
        String sql="SELECT r.room_id,r.game_id,r.play_version,r.state,r.route_endpoint FROM aoo_hall_room r JOIN aoo_hall_room_member m ON m.room_id=r.room_id WHERE m.account_id=? AND m.status='JOINED' AND r.state IN ('OPEN','PLAYING') ORDER BY r.updated_at DESC,r.room_id DESC LIMIT 1";
        try(Connection connection=source.getConnection();PreparedStatement query=connection.prepareStatement(sql)){query.setLong(1,accountId);try(ResultSet row=query.executeQuery()){if(!row.next())return Map.of("roomID",0,"practiceId",0,"gameType","NOT","roomKey",0);Map<String,Object> result=new LinkedHashMap<>();result.put("roomID",row.getLong("room_id"));result.put("roomKey",row.getLong("room_id"));result.put("practiceId",0);result.put("gameType",String.valueOf(row.getLong("game_id")));result.put("playVersion",row.getString("play_version"));result.put("state",row.getString("state"));result.put("route",row.getString("route_endpoint"));return result;}}
        catch(SQLException failure){throw new IllegalStateException("current hall room lookup failed",failure);}
    }
    private List<Map<String,Object>> clubInvitations(long accountId) {
        String sql="SELECT s.club_id,JSON_UNQUOTE(JSON_EXTRACT(s.state_json,'$.name')) club_name,i.token,i.expires_at FROM aoo_club_state s JOIN JSON_TABLE(s.state_json,'$.invites[*]' COLUMNS(token VARCHAR(64) PATH '$.token',player_id BIGINT PATH '$.playerId',expires_at DATETIME(3) PATH '$.expiresAt',accepted BOOLEAN PATH '$.accepted')) i WHERE i.player_id=? AND i.accepted=FALSE AND i.expires_at>CURRENT_TIMESTAMP(3) ORDER BY s.updated_at DESC";
        try(Connection connection=source.getConnection();PreparedStatement query=connection.prepareStatement(sql)){query.setLong(1,accountId);try(ResultSet rows=query.executeQuery()){List<Map<String,Object>> out=new ArrayList<>();while(rows.next())out.add(Map.of("clubId",rows.getLong("club_id"),"clubName",Objects.toString(rows.getString("club_name"),""),"token",rows.getString("token"),"expiresAt",rows.getString("expires_at")));return out;}}
        catch(SQLException failure){throw new IllegalStateException("club invitation lookup failed",failure);}
    }
    private List<Map<String,Object>> unionNotifications(long accountId) {
        String sql="SELECT id,type,payload,created_at FROM social_notification WHERE recipient_id=? AND type LIKE 'UNION%' ORDER BY id DESC LIMIT 100";
        try(Connection connection=source.getConnection();PreparedStatement query=connection.prepareStatement(sql)){query.setLong(1,accountId);try(ResultSet rows=query.executeQuery()){List<Map<String,Object>> out=new ArrayList<>();while(rows.next())out.add(Map.of("id",rows.getLong("id"),"type",rows.getString("type"),"payload",rows.getString("payload"),"createdAt",rows.getTimestamp("created_at").toInstant().toString()));return out;}}
        catch(SQLException failure){throw new IllegalStateException("union notification lookup failed",failure);}
    }
    private Map<String,Object> profile(long accountId) {
        String sql="SELECT a.account_id,a.guest,p.nickname,p.gender_code FROM aoo_account a LEFT JOIN player_profile p ON p.player_id=a.account_id WHERE a.account_id=?";
        try(Connection connection=source.getConnection();PreparedStatement query=connection.prepareStatement(sql)) {
            query.setLong(1,accountId);try(ResultSet rows=query.executeQuery()) {
                if(!rows.next())throw new SecurityException("ticket account no longer exists");
                String name=rows.getString("nickname");if(name==null||name.isBlank())name=(rows.getBoolean("guest")?"游客":"玩家")+accountId;
                Map<String,Object> result=new LinkedHashMap<>();result.put("pid",accountId);result.put("accountID",accountId);result.put("name",name);result.put("nickName",name);
                result.put("sex",gender(rows.getString("gender_code")));result.put("headImageUrl","");result.put("roomCard",0);result.put("diamond",0);result.put("gold",0);return result;
            }
        } catch(SecurityException failure) { throw failure; }
        catch(Exception failure) { throw new IllegalStateException("hall profile lookup failed",failure); }
    }
    private static int gender(String value) { return "MALE".equals(value)?1:"FEMALE".equals(value)?2:0; }
}
