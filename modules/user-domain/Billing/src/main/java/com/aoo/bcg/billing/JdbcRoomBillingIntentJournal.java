package com.aoo.bcg.billing;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Immutable intent journal only; it cannot confirm business success, charge, or refund. */
public final class JdbcRoomBillingIntentJournal{
    private static final String FIELDS="operation_key,command_hash,room_id,room_attempt,billing_unit,actor_id,payer_id,game_id,play_version,region_code,billing_kind,billing_context,payer_mode,measure_kind,measure_value,player_count,carry_minor,created_at";
    public record Entry(long sequence,RoomBillingIntent intent){
        public Entry{if(sequence<=0)throw new IllegalArgumentException("invalid intent sequence");Objects.requireNonNull(intent);}
    }
    public Entry append(Connection connection,RoomBillingIntent intent)throws SQLException{
        Objects.requireNonNull(connection,"connection");Objects.requireNonNull(intent,"intent");
        if(connection.getAutoCommit())throw new IllegalArgumentException("intent append requires caller transaction");
        var operation=intent.operation();var existing=find(connection,operation.operationKey(),true);
        if(existing!=null){same(existing,intent,null);return existing;}
        String sql="INSERT INTO aoo_room_billing_intent_journal("+FIELDS+") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try(var q=connection.prepareStatement(sql)){
            int i=1;q.setString(i++,operation.operationKey());q.setString(i++,operation.commandHash());q.setLong(i++,operation.roomId());
            q.setString(i++,operation.roomAttempt());q.setString(i++,operation.unitId());q.setLong(i++,operation.actorId());
            q.setLong(i++,operation.payerId());q.setLong(i++,operation.gameId());q.setString(i++,operation.playVersion());
            q.setString(i++,operation.regionCode());q.setString(i++,operation.kind().name());q.setString(i++,operation.context().name());
            q.setString(i++,operation.payerMode().name());q.setString(i++,operation.measureKind());q.setInt(i++,operation.measureValue());
            q.setInt(i++,operation.playerCount());q.setLong(i++,operation.carryMinor());q.setTimestamp(i,Timestamp.from(intent.createdAt()));q.executeUpdate();
        }catch(SQLException failure){
            if(failure.getSQLState()==null||!failure.getSQLState().startsWith("23"))throw failure;
            existing=find(connection,operation.operationKey(),true);if(existing==null)throw failure;
            same(existing,intent,failure);return existing;
        }
        return Objects.requireNonNull(find(connection,operation.operationKey(),true),"inserted intent missing");
    }
    public Entry find(Connection connection,String key)throws SQLException{
        if(key==null||!key.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("invalid intent key");
        return find(Objects.requireNonNull(connection,"connection"),key,false);
    }
    /**
     * Ordered visible rows, not a concurrent lossless recovery cursor: allocation order differs
     * from commit order. Complete recovery needs an external producer barrier, all writing
     * transactions settled, then a fresh scan from zero; this journal never asserts that barrier.
     */
    public List<Entry> scanAfter(Connection connection,long sequence,int limit)throws SQLException{
        Objects.requireNonNull(connection,"connection");
        if(sequence<0||limit<1||limit>200)throw new IllegalArgumentException("invalid intent scan bounds");
        List<Entry> rows=new ArrayList<>();
        try(var q=connection.prepareStatement("SELECT intent_sequence,"+FIELDS+" FROM aoo_room_billing_intent_journal WHERE intent_sequence>? ORDER BY intent_sequence LIMIT ?")){
            q.setLong(1,sequence);q.setInt(2,limit);try(var result=q.executeQuery()){while(result.next())rows.add(read(result));}
        }return List.copyOf(rows);
    }
    private Entry find(Connection connection,String key,boolean lock)throws SQLException{
        try(var q=connection.prepareStatement("SELECT intent_sequence,"+FIELDS+" FROM aoo_room_billing_intent_journal WHERE operation_key=?"+(lock?" FOR UPDATE":""))){
            q.setString(1,key);try(var result=q.executeQuery()){return result.next()?read(result):null;}
        }
    }
    private Entry read(ResultSet row)throws SQLException{
        var operation=new RoomChargeOperation(row.getLong("room_id"),row.getString("room_attempt"),row.getString("billing_unit"),
                row.getLong("actor_id"),row.getLong("payer_id"),row.getLong("game_id"),row.getString("play_version"),row.getString("region_code"),
                RoomChargeOperation.Kind.valueOf(row.getString("billing_kind")),RoomChargeOperation.Context.valueOf(row.getString("billing_context")),
                RoomChargeOperation.Payer.valueOf(row.getString("payer_mode")),row.getString("measure_kind"),row.getInt("measure_value"),row.getInt("player_count"),row.getLong("carry_minor"));
        if(!operation.operationKey().equals(row.getString("operation_key"))||!operation.commandHash().equals(row.getString("command_hash")))
            throw new IllegalStateException("stored billing intent identity mismatch");
        return new Entry(row.getLong("intent_sequence"),new RoomBillingIntent(operation,row.getTimestamp("created_at").toInstant()));
    }
    private static void same(Entry existing,RoomBillingIntent expected,SQLException cause){
        if(!existing.intent().operation().equals(expected.operation()))throw new IllegalArgumentException("billing intent key reused with different command",cause);
    }
}
