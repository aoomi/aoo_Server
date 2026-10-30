package com.aoo.bcg.billing;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.time.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;
import static com.aoo.bcg.billing.JdbcRoomBillingSourceBindingTest.*;
import static com.aoo.bcg.billing.RoomBillingSourceBinding.*;

final class JdbcRoomBoundChargeTest {
    private final AtomicLong ids=new AtomicLong(100);
    private JdbcDataSource pricing()throws Exception {
        var ds=fixture();try(var c=ds.getConnection();var s=c.createStatement()) {
            s.execute("CREATE TABLE aoo_currency_catalog(currency_code VARCHAR PRIMARY KEY,scope_type VARCHAR,status VARCHAR)");s.execute("INSERT INTO aoo_currency_catalog VALUES('ROOM_CARD','GLOBAL','ACTIVE')");
            s.execute("CREATE TABLE aoo_currency_balance(player_id BIGINT,currency VARCHAR,currency_scope_id BIGINT,balance BIGINT,version BIGINT,updated_at TIMESTAMP,PRIMARY KEY(player_id,currency,currency_scope_id))");s.execute("INSERT INTO aoo_currency_balance VALUES(7,'ROOM_CARD',0,500,0,CURRENT_TIMESTAMP)");
            s.execute("CREATE TABLE aoo_ledger(ledger_id BIGINT PRIMARY KEY,business_id VARCHAR UNIQUE,player_id BIGINT,currency VARCHAR,currency_scope_id BIGINT,delta BIGINT,balance_after BIGINT,reason_code VARCHAR,entry_status VARCHAR,channel_code VARCHAR,created_at TIMESTAMP)");
            s.execute("CREATE TABLE aoo_room_cost_publication(publication_id BIGINT PRIMARY KEY,environment VARCHAR)");s.execute("INSERT INTO aoo_room_cost_publication VALUES(1,'PRODUCTION')");
            s.execute("CREATE TABLE aoo_room_cost_active(pointer_name VARCHAR PRIMARY KEY,publication_id BIGINT)");s.execute("INSERT INTO aoo_room_cost_active VALUES('roomcard',1)");
            s.execute("CREATE TABLE aoo_room_cost_policy(publication_id BIGINT,policy_version BIGINT,content_hash VARCHAR,cost_minor BIGINT,currency_code VARCHAR,free_policy_ref VARCHAR,refund_policy_ref VARCHAR,tier_minor BIGINT,round_count BIGINT,game_id BIGINT,play_version VARCHAR,region_code VARCHAR,measure_kind VARCHAR,measure_value INT,player_count INT,payer_mode VARCHAR,billing_kind VARCHAR,billing_context VARCHAR,status VARCHAR,confirmation_status VARCHAR)");
            s.execute("INSERT INTO aoo_room_cost_policy VALUES(1,1,'"+"a".repeat(64)+"',100,'ROOM_CARD',NULL,'r5',0,0,1,'test-v1','TEST','ROUND_COUNT',8,4,'CLUB','FIXED','CLUB','ACTIVE','CONFIRMED')");
            s.execute("CREATE TABLE aoo_room_billing_reservation(request_id VARCHAR PRIMARY KEY,account_id BIGINT,room_id BIGINT,currency_code VARCHAR,amount BIGINT,state VARCHAR,created_at TIMESTAMP,updated_at TIMESTAMP,billing_operation_key VARCHAR UNIQUE,command_hash VARCHAR,actor_id BIGINT,room_attempt VARCHAR,billing_kind VARCHAR,billing_unit VARCHAR,policy_publication_id BIGINT,policy_snapshot VARCHAR,debit_business_id VARCHAR,refunded_minor BIGINT DEFAULT 0,confirmation_id VARCHAR,confirmation_hash VARCHAR)");
            s.execute("CREATE TABLE aoo_room_billing_refund(refund_id VARCHAR PRIMARY KEY,request_id VARCHAR,command_hash VARCHAR,amount BIGINT,credit_business_id VARCHAR,evidence_json VARCHAR,created_at TIMESTAMP)");
            ddl(c,"V20261016_91__room_card_pricing_publication_provenance.sql");
        }saveProvenance(ds,false);return ds;
    }
    private String policyKey(){var op=operation(7);return RoomCardPricingProvenance.policyKey(new RoomCardPricingImportValidator.PriceKey(new RoomCardPricingImportValidator.IdentityKey(op.gameId(),"TEST_ONLY",op.playVersion(),op.regionCode()),op.kind().name(),op.context().name(),op.playerCount(),op.measureKind(),op.measureValue(),op.payerMode().name(),0));}
    private void saveProvenance(JdbcDataSource ds,boolean free)throws Exception {
        try(var c=ds.getConnection()){c.setAutoCommit(false);new JdbcRoomCardPricingProvenanceRepository().save(c,new RoomCardPricingProvenance(1,policyKey(),"v1","a".repeat(64),"c".repeat(64),new RoomCardPricingImportValidator.SourceBinding(RoomCardPricingImportValidator.SourceClass.CURRENT_PUBLISHED,11,12,"b".repeat(64)),free?"FREE":null,free?TIME.minusSeconds(60):null,free?TIME.plusSeconds(60):null,false));c.commit();}
    }
    private JdbcRoomChargeCoordinator coordinator(JdbcDataSource ds,Instant at){return new JdbcRoomChargeCoordinator(ds,new JdbcBillingService(ds,ids::incrementAndGet,Clock.fixed(at,ZoneOffset.UTC)),new JdbcRoomCostPolicyRepository(ds,false),Clock.fixed(at,ZoneOffset.UTC),(c,o,s)->{assertFalse(c.getAutoCommit());assertEquals(7,o.payerId());});}
    private long balance(JdbcDataSource ds)throws Exception{try(var c=ds.getConnection();var s=c.createStatement();var r=s.executeQuery("SELECT balance FROM aoo_currency_balance WHERE player_id=7")){assertTrue(r.next());return r.getLong(1);}}
    @Test void durableBoundAuthorityChargesOnceAndReplaysOriginalPrice()throws Exception {
        var ds=pricing();var bound=seed(ds);var charge=coordinator(ds,TIME).reserve(bound);assertEquals(100,charge.amount());assertEquals(400,balance(ds));
        try(var c=ds.getConnection();var s=c.createStatement()){s.execute("UPDATE aoo_room_cost_policy SET cost_minor=300");}
        assertEquals(charge,coordinator(ds,TIME.plusSeconds(10)).reserve(bound));assertEquals(1,count(ds,"aoo_ledger"));
        try(var c=ds.getConnection()){c.setAutoCommit(false);assertThrows(IllegalArgumentException.class,()->repo().bind(c,key(),new RoomBillingIntent(operation(8),TIME),subject(),authority()));c.rollback();}
        var refund=coordinator(ds,TIME).refund(charge.requestId(),"original-refund",new JdbcRoomChargeCoordinator.RefundEvidence(JdbcRoomChargeCoordinator.RefundCause.BUSINESS_FAILED,"failed",RoomChargeOperation.hash("test")));assertEquals(100,refund.amount());assertEquals(500,balance(ds));
    }
    @Test void missingOrWrongPublishedProvenanceNeverDebits()throws Exception {
        for(String column:new String[]{"release_id","index_generation","source_hash"}) {
            var ds=pricing();var bound=seed(ds);try(var c=ds.getConnection();var s=c.createStatement()){s.execute("UPDATE aoo_room_cost_policy_provenance SET "+column+"="+(column.equals("source_hash")?"'"+"c".repeat(64)+"'":"99"));}
            assertThrows(IllegalArgumentException.class,()->coordinator(ds,TIME).reserve(bound));assertEquals(500,balance(ds));assertEquals(0,count(ds,"aoo_ledger"));assertEquals(0,count(ds,"aoo_room_billing_reservation"));
        }
        var ds=pricing();var bound=seed(ds);try(var c=ds.getConnection();var s=c.createStatement()){s.execute("DELETE FROM aoo_room_cost_policy_provenance");}assertThrows(IllegalArgumentException.class,()->coordinator(ds,TIME).reserve(bound));assertEquals(0,count(ds,"aoo_ledger"));
        assertThrows(IllegalArgumentException.class,()->new BoundIntent(key(),intent(),subject(),new AuthorityReference(11,12,"b".repeat(64),"OTHER")));
    }
    @Test void trustedCurrentClockRejectsFreeExpiredDespiteIntentTimeBeingValid()throws Exception {
        var ds=pricing();try(var c=ds.getConnection();var s=c.createStatement()){s.execute("DELETE FROM aoo_room_cost_policy_provenance");s.execute("UPDATE aoo_room_cost_policy SET cost_minor=0,free_policy_ref='FREE'");}saveProvenance(ds,true);var bound=seed(ds);
        assertThrows(IllegalArgumentException.class,()->coordinator(ds,TIME.plusSeconds(120)).reserve(bound));assertEquals(0,count(ds,"aoo_room_billing_reservation"));assertEquals(0,count(ds,"aoo_ledger"));
        assertEquals(0,coordinator(ds,TIME).reserve(bound).amount());assertEquals(0,count(ds,"aoo_ledger"));
    }
    @Test void abortFenceAndMissingDurableEnvelopeBlockReserve()throws Exception {
        var ds=pricing();var unclaimed=new BoundIntent(key(),intent(),subject(),authority());assertThrows(IllegalArgumentException.class,()->coordinator(ds,TIME).reserve(unclaimed));var bound=seed(ds);
        try(var c=ds.getConnection()){c.setAutoCommit(false);new JdbcRoomBillingBusinessOutcomeRepository(repo()).abortBeforeCommit(c,key());c.commit();}
        assertThrows(IllegalStateException.class,()->coordinator(ds,TIME).reserve(bound));assertEquals(500,balance(ds));assertEquals(0,count(ds,"aoo_ledger"));
    }
}
