package com.aoo.bcg.billing;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.regex.Pattern;

/** Reads confirmed room-card policies from the authoritative per-play-code registry. */
public final class JdbcRoomCostPolicyRepository {
    private static final Pattern PLAY_CODE=Pattern.compile("[a-z][a-z0-9]{2,31}");
    private static final String TABLE_PREFIX="aoo_room_card_price_";
    private final DataSource dataSource;
    private final boolean isolatedTestProfile;

    public JdbcRoomCostPolicyRepository(DataSource source,boolean isolatedTestProfile) {
        this.dataSource=Objects.requireNonNull(source);this.isolatedTestProfile=isolatedTestProfile;
    }

    public record PolicySnapshot(long publicationId,long policyVersion,String contentHash,long unitCost,
            long amount,String currency,String freeRef,String refundRef,long tierMinor) {
        String json() {
            return "{\"publicationId\":"+publicationId+",\"policyVersion\":"+policyVersion
                    +",\"contentHash\":"+RoomChargeOperation.json(contentHash)+",\"unitCost\":"+unitCost
                    +",\"amount\":"+amount+",\"currency\":"+RoomChargeOperation.json(currency)
                    +",\"freeRef\":"+RoomChargeOperation.json(freeRef)+",\"refundRef\":"+RoomChargeOperation.json(refundRef)
                    +",\"tierMinor\":"+tierMinor+"}";
        }
    }

    private record PlayRegistry(String playCode,String tableName,long releaseId,long indexGeneration,
            String runtimeSourceHash) {}
    private record Selected(PolicySnapshot snapshot,int rank) {}

    public PolicySnapshot load(Connection connection,RoomChargeOperation operation)throws SQLException {
        return selectPolicy(connection,operation,null,null);
    }

    private PolicySnapshot selectPolicy(Connection connection,RoomChargeOperation operation,Long publicationId,
            RoomBillingSourceBinding.AuthorityReference authority)throws SQLException {
        Objects.requireNonNull(connection);Objects.requireNonNull(operation);
        if(isolatedTestProfile)requireIsolated(connection);
        PlayRegistry registry=loadRegistry(connection,operation);
        if(publicationId!=null&&publicationId.longValue()!=registry.releaseId())
            throw new IllegalArgumentException("billing publication is not active for play code");
        if(authority!=null)requireAuthority(registry,authority);

        String sql="SELECT policy_version,content_hash,amount_minor,currency_code,free_policy_ref,refund_policy_ref,"
                +"release_id,index_generation,runtime_source_hash,match_scope FROM `"+registry.tableName()+"` "
                +"WHERE play_code=? AND play_version=? AND payer_mode=? AND billing_kind=? AND billing_context=? "
                +"AND status='ACTIVE' AND confirmation_status='CONFIRMED' AND "
                +"((match_scope='EXACT' AND player_count=? AND measure_kind=? AND measure_value=?) OR "
                +"(match_scope='PLAY_DEFAULT' AND player_count IS NULL AND measure_kind IS NULL AND measure_value IS NULL)) "
                +"ORDER BY CASE match_scope WHEN 'EXACT' THEN 0 ELSE 1 END";
        try(var statement=connection.prepareStatement(sql)) {
            int parameter=1;
            statement.setString(parameter++,registry.playCode());statement.setString(parameter++,operation.playVersion());
            statement.setString(parameter++,operation.payerMode().name());statement.setString(parameter++,operation.kind().name());
            statement.setString(parameter++,operation.context().name());statement.setInt(parameter++,operation.playerCount());
            statement.setString(parameter++,operation.measureKind());statement.setInt(parameter,operation.measureValue());
            try(var rows=statement.executeQuery()) {
                Selected selected=null;
                while(rows.next()) {
                    Selected candidate=read(rows,registry);
                    if(selected==null)selected=candidate;
                    else if(candidate.rank()==selected.rank())throw new IllegalArgumentException("overlapping published policies");
                    else break;
                }
                if(selected==null)throw new IllegalArgumentException("missing confirmed room-card policy");
                return selected.snapshot();
            }
        }
    }

    private PlayRegistry loadRegistry(Connection connection,RoomChargeOperation operation)throws SQLException {
        String sql="SELECT play_code,table_name,release_id,index_generation,runtime_source_hash "
                +"FROM aoo_room_card_play_registry WHERE game_id=? AND play_version=? AND status='ACTIVE'";
        try(var statement=connection.prepareStatement(sql)) {
            statement.setLong(1,operation.gameId());statement.setString(2,operation.playVersion());
            try(var rows=statement.executeQuery()) {
                if(!rows.next())throw new IllegalArgumentException("missing active room-card play-code registry");
                PlayRegistry registry=new PlayRegistry(rows.getString(1),rows.getString(2),rows.getLong(3),
                        rows.getLong(4),rows.getString(5));
                if(rows.next())throw new IllegalArgumentException("ambiguous active room-card play-code registry");
                validateRegistry(registry);return registry;
            }
        }
    }

    private static void validateRegistry(PlayRegistry registry) {
        String playCode=registry.playCode(),hash=registry.runtimeSourceHash();
        if(playCode==null||!PLAY_CODE.matcher(playCode).matches()
                ||!Objects.equals(TABLE_PREFIX+playCode,registry.tableName())
                ||registry.releaseId()<=0||registry.indexGeneration()<=0
                ||hash==null||!hash.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("invalid room-card play-code registry");
    }

    private static Selected read(ResultSet row,PlayRegistry registry)throws SQLException {
        long version=row.getLong(1),cost=row.getLong(3),release=row.getLong(7),generation=row.getLong(8);
        String hash=row.getString(2),currency=row.getString(4),free=row.getString(5),refund=row.getString(6);
        String sourceHash=row.getString(9),scope=row.getString(10);
        int rank="EXACT".equals(scope)?0:"PLAY_DEFAULT".equals(scope)?1:-1;
        if(version<=0||cost<0||!"ROOM_CARD".equals(currency)||rank<0
                ||hash==null||!hash.matches("[0-9a-f]{64}")||refund==null||refund.isBlank()
                ||cost==0&&(free==null||free.isBlank())
                ||release!=registry.releaseId()||generation!=registry.indexGeneration()
                ||!Objects.equals(sourceHash,registry.runtimeSourceHash()))
            throw new IllegalArgumentException("invalid or ambiguous published policy");
        return new Selected(new PolicySnapshot(release,version,hash,cost,cost,currency,free,refund,0),rank);
    }

    /** ACTIVE release provenance is bound to the trusted authority tuple. */
    public PolicySnapshot load(Connection connection,RoomBillingSourceBinding.BoundIntent bound,java.time.Instant at)throws SQLException {
        Objects.requireNonNull(bound);return load(connection,bound.intent().operation(),bound.authority(),at);
    }

    PolicySnapshot load(Connection connection,RoomChargeOperation operation,
            RoomBillingSourceBinding.AuthorityReference authority,java.time.Instant at)throws SQLException {
        Objects.requireNonNull(operation);Objects.requireNonNull(authority);Objects.requireNonNull(at);
        return selectPolicy(connection,operation,null,authority);
    }

    PolicySnapshot loadAtPublication(Connection connection,RoomChargeOperation operation,
            RoomBillingSourceBinding.AuthorityReference authority,java.time.Instant at,long publicationId)throws SQLException {
        if(publicationId<=0)throw new IllegalArgumentException("billing publication authority mismatch");
        Objects.requireNonNull(at);return selectPolicy(connection,operation,publicationId,Objects.requireNonNull(authority));
    }

    private static void requireAuthority(PlayRegistry registry,RoomBillingSourceBinding.AuthorityReference authority) {
        if(registry.releaseId()!=authority.releaseId()||registry.indexGeneration()!=authority.indexGeneration()
                ||!Objects.equals(registry.runtimeSourceHash(),authority.runtimeSourceHash()))
            throw new IllegalArgumentException("room-card pricing authority mismatch");
    }

    static void requireIsolated(Connection connection)throws SQLException {
        try(var statement=connection.createStatement();var row=statement.executeQuery("SELECT DATABASE()")) {
            if(!row.next()||!"aoo_fangka_isolated_25162442_r1".equals(row.getString(1)))
                throw new IllegalStateException("isolated billing database required");
        }
        String url=connection.getMetaData().getURL();
        if(url==null||!url.startsWith("jdbc:mysql://127.0.0.1:")||url.contains("password="))
            throw new IllegalStateException("private local MySQL endpoint required");
    }
}
