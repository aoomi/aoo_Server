package com.aoo.bcg.billing;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/** Reads one explicit immutable publication; never falls back to legacy or live spreadsheets. */
public final class JdbcRoomCostPolicyRepository {
    private final DataSource dataSource;
    private final boolean isolatedTestProfile;
    public JdbcRoomCostPolicyRepository(DataSource source,boolean isolatedTestProfile) {
        this.dataSource=Objects.requireNonNull(source); this.isolatedTestProfile=isolatedTestProfile;
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
    public PolicySnapshot load(Connection connection,RoomChargeOperation operation)throws SQLException {
        return selectPolicy(connection,operation,null);
    }
    private PolicySnapshot selectPolicy(Connection connection,RoomChargeOperation operation,Long publicationId)throws SQLException {
        Objects.requireNonNull(connection); Objects.requireNonNull(operation);
        if(isolatedTestProfile) requireIsolated(connection);
        String sql="SELECT p.publication_id,p.policy_version,p.content_hash,p.cost_minor,p.currency_code,"
                +"p.free_policy_ref,p.refund_policy_ref,p.tier_minor,p.round_count,v.environment "
                +(publicationId==null?"FROM aoo_room_cost_active a JOIN aoo_room_cost_publication v ON v.publication_id=a.publication_id JOIN aoo_room_cost_policy p ON p.publication_id=a.publication_id WHERE a.pointer_name='roomcard' AND ":
                    "FROM aoo_room_cost_publication v JOIN aoo_room_cost_policy p ON p.publication_id=v.publication_id WHERE v.publication_id=? AND ")
                +"p.game_id=? AND p.play_version=? AND p.region_code=? "
                +"AND p.measure_kind=? AND p.measure_value=? AND p.player_count=? AND p.payer_mode=? "
                +"AND p.billing_kind=? AND p.billing_context=? AND p.status='ACTIVE' AND p.confirmation_status='CONFIRMED'";
        try(var statement=connection.prepareStatement(sql)) {
            int parameter=1;if(publicationId!=null)statement.setLong(parameter++,publicationId);
            statement.setLong(parameter++,operation.gameId());statement.setString(parameter++,operation.playVersion());
            statement.setString(parameter++,operation.regionCode());statement.setString(parameter++,operation.measureKind());
            statement.setInt(parameter++,operation.measureValue());statement.setInt(parameter++,operation.playerCount());
            statement.setString(parameter++,operation.payerMode().name());statement.setString(parameter++,operation.kind().name());
            statement.setString(parameter,operation.context().name());
            try(var row=statement.executeQuery()) {
                if(!row.next())throw new IllegalArgumentException("missing confirmed room-card policy");
                long publication=row.getLong(1),version=row.getLong(2),cost=row.getLong(4),tier=row.getLong(8);
                String hash=row.getString(3),currency=row.getString(5),free=row.getString(6),refund=row.getString(7);
                String environment=row.getString(10);
                if(!("PRODUCTION".equals(environment) || isolatedTestProfile && "TEST_ONLY".equals(environment)))
                    throw new IllegalArgumentException("test publication forbidden in production profile");
                if(publication<=0 || version<=0 || cost<0 || !"ROOM_CARD".equals(currency)
                        ||hash==null||!hash.matches("[0-9a-f]{64}")||refund==null||refund.isBlank()
                        ||cost==0&&(free==null||free.isBlank()))
                    throw new IllegalArgumentException("invalid or ambiguous published policy");
                long amount=cost;
                if(operation.kind()==RoomChargeOperation.Kind.CARRY) {
                    if(tier!=operation.measureValue()||tier!=row.getLong(9)||tier<=0)
                        throw new IllegalArgumentException("carry tier identity mismatch");
                    amount=Math.multiplyExact(1+(operation.carryMinor()-1)/tier,cost);
                } else if(tier!=0)throw new IllegalArgumentException("unexpected carry tier");
                if(row.next())throw new IllegalArgumentException("overlapping published policies");
                return new PolicySnapshot(publication,version,hash,cost,amount,currency,free,refund,tier);
            }
        }
    }
    /** New production envelope: published provenance and free windows use the trusted current clock. */
    public PolicySnapshot load(Connection connection,RoomBillingSourceBinding.BoundIntent bound,java.time.Instant at)throws SQLException {
        Objects.requireNonNull(bound);return load(connection,bound.intent().operation(),bound.authority(),at);
    }
    PolicySnapshot load(Connection connection,RoomChargeOperation operation,
            RoomBillingSourceBinding.AuthorityReference authority,java.time.Instant at)throws SQLException {
        Objects.requireNonNull(operation);Objects.requireNonNull(authority);Objects.requireNonNull(at);
        if(!authority.classificationRegionCode().equals(operation.regionCode()))throw new IllegalArgumentException("billing authority region mismatch");
        return validateProvenance(connection,operation,authority,at,load(connection,operation));
    }
    PolicySnapshot loadAtPublication(Connection connection,RoomChargeOperation operation,
            RoomBillingSourceBinding.AuthorityReference authority,java.time.Instant at,long publicationId)throws SQLException {
        if(publicationId<=0||!authority.classificationRegionCode().equals(operation.regionCode()))throw new IllegalArgumentException("billing publication authority mismatch");
        return validateProvenance(connection,operation,authority,at,selectPolicy(connection,operation,publicationId));
    }
    private PolicySnapshot validateProvenance(Connection connection,RoomChargeOperation operation,
            RoomBillingSourceBinding.AuthorityReference authority,java.time.Instant at,PolicySnapshot selected)throws SQLException {
        Objects.requireNonNull(at);
        // This is the same dimension order as RoomCardPricingProvenance.policyKey; no game-code inference.
        String key=RoomChargeOperation.hash(operation.gameId(),operation.playVersion(),operation.regionCode(),operation.kind(),operation.context(),
                operation.playerCount(),operation.measureKind(),operation.measureValue(),operation.payerMode(),operation.kind()==RoomChargeOperation.Kind.CARRY?(long)operation.measureValue():0L);
        var provenance=new JdbcRoomCardPricingProvenanceRepository().load(connection,selected.publicationId(),key);
        provenance.requireCurrent(new RoomCardPricingImportValidator.SourceBinding(RoomCardPricingImportValidator.SourceClass.CURRENT_PUBLISHED,
                authority.releaseId(),authority.indexGeneration(),authority.runtimeSourceHash()));
        if(!Objects.equals(provenance.freeRef(),selected.freeRef()))throw new IllegalArgumentException("published free reference mismatch");
        provenance.requireValidAt(at,selected.amount()==0);return selected;
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
