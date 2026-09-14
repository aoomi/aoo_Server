package com.aoo.bcg.social;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import static com.aoo.bcg.social.SocialModels.*;

/** Durable repository. Every multi-row social transition is committed atomically. */
public final class JdbcSocialRepository {
    private final DataSource dataSource; private final Clock clock;
    public JdbcSocialRepository(DataSource dataSource,Clock clock){this.dataSource=Objects.requireNonNull(dataSource);this.clock=Objects.requireNonNull(clock);}

    public FriendRequest request(long from,long to){return tx(c->{
        if(blocked(c,from,to))throw new SecurityException("friend request forbidden by block policy");
        if(friends(c,from,to))throw new IllegalStateException("already friends");
        try(var q=c.prepareStatement("SELECT id,requester_id,recipient_id,status,created_at,decided_at FROM social_friend_request WHERE requester_id=? AND recipient_id=? AND status='PENDING'")){q.setLong(1,from);q.setLong(2,to);try(var r=q.executeQuery()){if(r.next())return friendRequest(r);}}
        try(var q=c.prepareStatement("INSERT INTO social_friend_request(requester_id,recipient_id,status,created_at) VALUES(?,?,'PENDING',?)",Statement.RETURN_GENERATED_KEYS)){q.setLong(1,from);q.setLong(2,to);q.setTimestamp(3,Timestamp.from(clock.instant()));q.executeUpdate();try(var k=q.getGeneratedKeys()){if(!k.next())throw new SQLException("friend request id not returned");return findRequest(c,k.getLong(1));}}
    });}
    public FriendRequest decide(long actor,long requestId,boolean accept){return tx(c->{
        FriendRequest r;try(var q=c.prepareStatement("SELECT id,requester_id,recipient_id,status,created_at,decided_at FROM social_friend_request WHERE id=? FOR UPDATE")){q.setLong(1,requestId);try(var x=q.executeQuery()){if(!x.next())throw new IllegalArgumentException("friend request not found");r=friendRequest(x);}}
        if(r.recipientId()!=actor)throw new SecurityException("only recipient may decide request");if(!r.status().equals("PENDING"))throw new IllegalStateException("friend request already decided");
        if(accept&&blocked(c,r.requesterId(),r.recipientId()))throw new SecurityException("friendship forbidden by block policy");
        String status=accept?"ACCEPTED":"REJECTED";try(var q=c.prepareStatement("UPDATE social_friend_request SET status=?,decided_at=? WHERE id=?")){q.setString(1,status);q.setTimestamp(2,Timestamp.from(clock.instant()));q.setLong(3,requestId);q.executeUpdate();}
        if(accept){long low=Math.min(actor,r.requesterId()),high=Math.max(actor,r.requesterId());try(var q=c.prepareStatement("INSERT INTO social_friendship(user_low,user_high,created_at) VALUES(?,?,?)")){q.setLong(1,low);q.setLong(2,high);q.setTimestamp(3,Timestamp.from(clock.instant()));q.executeUpdate();}}
        return findRequest(c,requestId);
    });}
    public void deleteFriend(long actor,long other){tx(c->{long low=Math.min(actor,other),high=Math.max(actor,other);try(var q=c.prepareStatement("DELETE FROM social_friendship WHERE user_low=? AND user_high=?")){q.setLong(1,low);q.setLong(2,high);if(q.executeUpdate()==0)throw new IllegalArgumentException("friendship not found");}return null;});}
    public void block(long actor,long other){tx(c->{
        try(var q=c.prepareStatement("INSERT INTO social_block(blocker_id,blocked_id,created_at) VALUES(?,?,?)")){q.setLong(1,actor);q.setLong(2,other);q.setTimestamp(3,Timestamp.from(clock.instant()));try{q.executeUpdate();}catch(SQLIntegrityConstraintViolationException ignored){}}
        long low=Math.min(actor,other),high=Math.max(actor,other);try(var q=c.prepareStatement("DELETE FROM social_friendship WHERE user_low=? AND user_high=?")){q.setLong(1,low);q.setLong(2,high);q.executeUpdate();}
        try(var q=c.prepareStatement("UPDATE social_friend_request SET status='CANCELLED',decided_at=? WHERE status='PENDING' AND ((requester_id=? AND recipient_id=?) OR (requester_id=? AND recipient_id=?))")){q.setTimestamp(1,Timestamp.from(clock.instant()));q.setLong(2,actor);q.setLong(3,other);q.setLong(4,other);q.setLong(5,actor);q.executeUpdate();}return null;
    });}
    public void unblock(long actor,long other){execute("DELETE FROM social_block WHERE blocker_id=? AND blocked_id=?",actor,other);}
    public boolean areFriends(long a,long b){return read(c->friends(c,a,b));}
    public boolean isBlocked(long a,long b){return read(c->blocked(c,a,b));}
    public List<Friend> friends(long actor){return read(c->{
        List<Friend> out=new ArrayList<>();
        try(var q=c.prepareStatement("SELECT CASE WHEN f.user_low=? THEN f.user_high ELSE f.user_low END account_id,p.state,p.room_id,p.last_seen_at,p.visibility FROM social_friendship f LEFT JOIN social_presence p ON p.account_id=CASE WHEN f.user_low=? THEN f.user_high ELSE f.user_low END WHERE f.user_low=? OR f.user_high=? ORDER BY account_id")){
            q.setLong(1,actor);q.setLong(2,actor);q.setLong(3,actor);q.setLong(4,actor);
            try(var r=q.executeQuery()){while(r.next()){
                String state=r.getString("state");String visibility=r.getString("visibility");
                Long roomId=(Long)r.getObject("room_id");Timestamp lastSeen=r.getTimestamp("last_seen_at");
                if(state==null||"NOBODY".equals(visibility)){state="OFFLINE";roomId=null;lastSeen=null;}
                out.add(new Friend(r.getLong("account_id"),state,roomId,lastSeen==null?null:lastSeen.toInstant()));
            }}
        }return out;
    });}
    public List<FriendRequest> pendingRequests(long actor){return read(c->{List<FriendRequest> out=new ArrayList<>();try(var q=c.prepareStatement("SELECT id,requester_id,recipient_id,status,created_at,decided_at FROM social_friend_request WHERE recipient_id=? AND status='PENDING' ORDER BY id")){q.setLong(1,actor);try(var r=q.executeQuery()){while(r.next())out.add(friendRequest(r));}}return out;});}

    public void setPresence(long actor,String state,Long roomId,String visibility){tx(c->{
        if(!List.of("ONLINE","AWAY","OFFLINE").contains(state))throw new IllegalArgumentException("invalid presence state");
        if(!List.of("FRIENDS","NOBODY").contains(visibility))throw new IllegalArgumentException("invalid presence visibility");
        int n;try(var q=c.prepareStatement("UPDATE social_presence SET state=?,room_id=?,visibility=?,last_seen_at=? WHERE account_id=?")){bindPresence(q,state,roomId,visibility,actor);n=q.executeUpdate();}
        if(n==0)try(var q=c.prepareStatement("INSERT INTO social_presence(state,room_id,visibility,last_seen_at,account_id) VALUES(?,?,?,?,?)")){bindPresence(q,state,roomId,visibility,actor);q.executeUpdate();}return null;
    });}
    private void bindPresence(PreparedStatement q,String state,Long roomId,String visibility,long actor)throws SQLException{q.setString(1,state);if(roomId==null)q.setNull(2,Types.BIGINT);else q.setLong(2,roomId);q.setString(3,visibility);q.setTimestamp(4,Timestamp.from(clock.instant()));q.setLong(5,actor);}
    public Presence presence(long viewer,long subject){return read(c->{if(viewer!=subject&&(blocked(c,viewer,subject)||!friends(c,viewer,subject)))throw new SecurityException("presence is private");try(var q=c.prepareStatement("SELECT account_id,state,room_id,last_seen_at,visibility FROM social_presence WHERE account_id=?")){q.setLong(1,subject);try(var r=q.executeQuery()){if(!r.next()||(!viewerEquals(viewer,subject)&&"NOBODY".equals(r.getString("visibility"))))throw new SecurityException("presence is private");return new Presence(r.getLong(1),r.getString(2),(Long)r.getObject(3),r.getTimestamp(4).toInstant());}}});}
    private static boolean viewerEquals(long a,long b){return a==b;}

    public Notification notify(long recipient,String type,String dedupeKey,String payload){return tx(c->{
        try(var q=c.prepareStatement("INSERT INTO social_notification(recipient_id,type,dedupe_key,payload,created_at) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){q.setLong(1,recipient);q.setString(2,type);q.setString(3,dedupeKey);q.setString(4,payload);q.setTimestamp(5,Timestamp.from(clock.instant()));try{q.executeUpdate();try(var k=q.getGeneratedKeys()){k.next();return findNotification(c,k.getLong(1));}}catch(SQLIntegrityConstraintViolationException duplicate){try(var existing=c.prepareStatement("SELECT id,recipient_id,type,payload,created_at FROM social_notification WHERE recipient_id=? AND dedupe_key=?")){existing.setLong(1,recipient);existing.setString(2,dedupeKey);try(var r=existing.executeQuery()){if(!r.next())throw duplicate;return notification(r);}}}}
    });}
    public Feed<Notification> notifications(long actor,long after,int limit){return read(c->{List<Notification> out=new ArrayList<>();long next=after;try(var q=c.prepareStatement("SELECT id,recipient_id,type,payload,created_at FROM social_notification WHERE recipient_id=? AND id>? ORDER BY id LIMIT ?")){q.setLong(1,actor);q.setLong(2,after);q.setInt(3,limit);try(var r=q.executeQuery()){while(r.next()){var n=notification(r);out.add(n);next=n.id();}}}return new Feed<>(out,next,readCursor(c,actor,"NOTIFICATION"));});}
    public Mail mail(long recipient,String dedupeKey,String subject,String body){return tx(c->{try(var q=c.prepareStatement("INSERT INTO social_mail(recipient_id,dedupe_key,subject,body,created_at) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){q.setLong(1,recipient);q.setString(2,dedupeKey);q.setString(3,subject);q.setString(4,body);q.setTimestamp(5,Timestamp.from(clock.instant()));try{q.executeUpdate();try(var k=q.getGeneratedKeys()){k.next();return findMail(c,k.getLong(1));}}catch(SQLIntegrityConstraintViolationException duplicate){try(var e=c.prepareStatement("SELECT id,recipient_id,subject,body,created_at FROM social_mail WHERE recipient_id=? AND dedupe_key=?")){e.setLong(1,recipient);e.setString(2,dedupeKey);try(var r=e.executeQuery()){if(!r.next())throw duplicate;return mail(r);}}}}});}
    public Feed<Mail> mails(long actor,long after,int limit){return read(c->{List<Mail> out=new ArrayList<>();long next=after;try(var q=c.prepareStatement("SELECT id,recipient_id,subject,body,created_at FROM social_mail WHERE recipient_id=? AND id>? ORDER BY id LIMIT ?")){q.setLong(1,actor);q.setLong(2,after);q.setInt(3,limit);try(var r=q.executeQuery()){while(r.next()){var m=mail(r);out.add(m);next=m.id();}}}return new Feed<>(out,next,readCursor(c,actor,"MAIL"));});}
    public Mail mailDetail(long actor,long id){return read(c->{try(var q=c.prepareStatement("SELECT id,recipient_id,subject,body,created_at FROM social_mail WHERE id=? AND recipient_id=?")){q.setLong(1,id);q.setLong(2,actor);try(var r=q.executeQuery()){if(!r.next())throw new IllegalArgumentException("mail not found");return mail(r);}}});}
    public Feed<Notice> notices(long actor,long after,int limit){return read(c->{List<Notice> out=new ArrayList<>();long next=after;try(var q=c.prepareStatement("SELECT id,title,content,starts_at,ends_at,updated_at FROM social_notice WHERE published=TRUE AND id>? AND starts_at<=? AND ends_at>? ORDER BY id LIMIT ?")){var now=Timestamp.from(clock.instant());q.setLong(1,after);q.setTimestamp(2,now);q.setTimestamp(3,now);q.setInt(4,limit);try(var r=q.executeQuery()){while(r.next()){var n=notice(r);out.add(n);next=n.id();}}}return new Feed<>(out,next,readCursor(c,actor,"NOTICE"));});}
    public RedDots redDots(long actor){return read(c->{long mail=count(c,"SELECT COUNT(*) FROM social_mail WHERE recipient_id=? AND id>?",actor,readCursor(c,actor,"MAIL"));long notification=count(c,"SELECT COUNT(*) FROM social_notification WHERE recipient_id=? AND id>?",actor,readCursor(c,actor,"NOTIFICATION"));long notice=countNotice(c,readCursor(c,actor,"NOTICE"));return new RedDots(mail,notification,notice,mail+notification+notice);});}
    public long advanceReadCursor(long actor,String stream,long cursor){return tx(c->{if(!List.of("NOTIFICATION","MAIL","NOTICE").contains(stream)||cursor<0)throw new IllegalArgumentException("invalid read cursor");long current=readCursor(c,actor,stream);if(cursor<=current)return current;try(var q=c.prepareStatement("UPDATE social_read_cursor SET cursor_id=?,updated_at=? WHERE account_id=? AND stream=?")){q.setLong(1,cursor);q.setTimestamp(2,Timestamp.from(clock.instant()));q.setLong(3,actor);q.setString(4,stream);if(q.executeUpdate()==0)try(var i=c.prepareStatement("INSERT INTO social_read_cursor(cursor_id,updated_at,account_id,stream) VALUES(?,?,?,?)")){i.setLong(1,cursor);i.setTimestamp(2,Timestamp.from(clock.instant()));i.setLong(3,actor);i.setString(4,stream);i.executeUpdate();}}return cursor;});}

    private long readCursor(Connection c,long actor,String stream)throws SQLException{try(var q=c.prepareStatement("SELECT cursor_id FROM social_read_cursor WHERE account_id=? AND stream=?")){q.setLong(1,actor);q.setString(2,stream);try(var r=q.executeQuery()){return r.next()?r.getLong(1):0;}}}
    private FriendRequest findRequest(Connection c,long id)throws SQLException{try(var q=c.prepareStatement("SELECT id,requester_id,recipient_id,status,created_at,decided_at FROM social_friend_request WHERE id=?")){q.setLong(1,id);try(var r=q.executeQuery()){if(!r.next())throw new SQLException("friend request vanished");return friendRequest(r);}}}
    private Notification findNotification(Connection c,long id)throws SQLException{try(var q=c.prepareStatement("SELECT id,recipient_id,type,payload,created_at FROM social_notification WHERE id=?")){q.setLong(1,id);try(var r=q.executeQuery()){r.next();return notification(r);}}}
    private Mail findMail(Connection c,long id)throws SQLException{try(var q=c.prepareStatement("SELECT id,recipient_id,subject,body,created_at FROM social_mail WHERE id=?")){q.setLong(1,id);try(var r=q.executeQuery()){r.next();return mail(r);}}}
    private static FriendRequest friendRequest(ResultSet r)throws SQLException{Timestamp d=r.getTimestamp(6);return new FriendRequest(r.getLong(1),r.getLong(2),r.getLong(3),r.getString(4),r.getTimestamp(5).toInstant(),d==null?null:d.toInstant());}
    private static Notification notification(ResultSet r)throws SQLException{return new Notification(r.getLong(1),r.getLong(2),r.getString(3),r.getString(4),r.getTimestamp(5).toInstant());}
    private static Mail mail(ResultSet r)throws SQLException{return new Mail(r.getLong(1),r.getLong(2),r.getString(3),r.getString(4),r.getTimestamp(5).toInstant());}
    private static Notice notice(ResultSet r)throws SQLException{return new Notice(r.getLong(1),r.getString(2),r.getString(3),r.getTimestamp(4).toInstant(),r.getTimestamp(5).toInstant(),r.getTimestamp(6).toInstant());}
    private static long count(Connection c,String sql,long actor,long cursor)throws SQLException{try(var q=c.prepareStatement(sql)){q.setLong(1,actor);q.setLong(2,cursor);try(var r=q.executeQuery()){r.next();return r.getLong(1);}}}
    private long countNotice(Connection c,long cursor)throws SQLException{try(var q=c.prepareStatement("SELECT COUNT(*) FROM social_notice WHERE published=TRUE AND id>? AND starts_at<=? AND ends_at>?")){var now=Timestamp.from(clock.instant());q.setLong(1,cursor);q.setTimestamp(2,now);q.setTimestamp(3,now);try(var r=q.executeQuery()){r.next();return r.getLong(1);}}}
    private static boolean friends(Connection c,long a,long b)throws SQLException{long low=Math.min(a,b),high=Math.max(a,b);try(var q=c.prepareStatement("SELECT 1 FROM social_friendship WHERE user_low=? AND user_high=?")){q.setLong(1,low);q.setLong(2,high);try(var r=q.executeQuery()){return r.next();}}}
    private static boolean blocked(Connection c,long a,long b)throws SQLException{try(var q=c.prepareStatement("SELECT 1 FROM social_block WHERE (blocker_id=? AND blocked_id=?) OR (blocker_id=? AND blocked_id=?)")){q.setLong(1,a);q.setLong(2,b);q.setLong(3,b);q.setLong(4,a);try(var r=q.executeQuery()){return r.next();}}}
    private void execute(String sql,long a,long b){tx(c->{try(var q=c.prepareStatement(sql)){q.setLong(1,a);q.setLong(2,b);q.executeUpdate();}return null;});}
    private <T>T read(Sql<T> task){try(var c=dataSource.getConnection()){return task.run(c);}catch(SQLException e){throw new IllegalStateException("social persistence unavailable",e);}}
    private <T>T tx(Sql<T> task){try(var c=dataSource.getConnection()){c.setAutoCommit(false);try{T value=task.run(c);c.commit();return value;}catch(Exception e){c.rollback();throw e;}}catch(SecurityException|IllegalArgumentException|IllegalStateException e){throw e;}catch(SQLException e){throw new IllegalStateException("social persistence unavailable",e);}}
    @FunctionalInterface private interface Sql<T>{T run(Connection c)throws SQLException;}
}
