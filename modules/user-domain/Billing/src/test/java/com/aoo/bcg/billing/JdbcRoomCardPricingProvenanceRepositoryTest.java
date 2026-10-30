package com.aoo.bcg.billing;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.Instant;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import com.aoo.bcg.billing.RoomCardPricingImportValidator.*;

class JdbcRoomCardPricingProvenanceRepositoryTest{
    private static final String H="a".repeat(64),KEY="b".repeat(64);
    private static final Instant FROM=Instant.parse("2026-01-01T00:00:00.123Z"),UNTIL=FROM.plusSeconds(60);
    private SourceBinding binding(){return new SourceBinding(SourceClass.CURRENT_PUBLISHED,7,9,"legacy-source-Hash");}
    private RoomCardPricingProvenance snapshot(String version,String ref,Instant from,Instant until,boolean always){return new RoomCardPricingProvenance(1,KEY,version,H,H,binding(),ref,from,until,always);}
    private JdbcDataSource source()throws Exception{
        var source=new JdbcDataSource();source.setURL("jdbc:h2:mem:provenance"+System.nanoTime()+";MODE=MySQL;DB_CLOSE_DELAY=-1");
        try(var c=source.getConnection();var q=c.createStatement()){
            q.execute("CREATE TABLE aoo_room_cost_publication(publication_id BIGINT UNSIGNED PRIMARY KEY)");q.execute("INSERT INTO aoo_room_cost_publication VALUES(1)");
            String ddl=Files.readString(Path.of(System.getProperty("aoo.provenance.ddl")));q.execute(ddl);
        }return source;
    }
    private long count(Connection c,String table)throws Exception{try(var q=c.createStatement();var rows=q.executeQuery("SELECT COUNT(*) FROM "+table)){rows.next();return rows.getLong(1);}}
    @Test void metadataRoundTripsAndCannotRewriteSamePublishedKey()throws Exception{
        var source=source();var repository=new JdbcRoomCardPricingProvenanceRepository();var value=snapshot("confirmed-v1","FREE1",FROM,UNTIL,false);
        try(var c=source.getConnection()){
            assertThrows(IllegalArgumentException.class,()->repository.save(c,value));c.setAutoCommit(false);
            repository.save(c,value);repository.save(c,value);assertEquals(value,repository.load(c,1,KEY));
            assertThrows(IllegalArgumentException.class,()->repository.save(c,snapshot("confirmed-v2","FREE1",FROM,UNTIL,false)));
            assertFalse(c.getAutoCommit());assertEquals(1,count(c,"aoo_room_cost_policy_provenance"));assertEquals(1,count(c,"aoo_room_cost_publication"));c.commit();
        }
        try(var c=source.getConnection()){assertEquals(value,repository.load(c,1,KEY));}
    }
    @Test void callerRollbackAndMissingMetadataNeverFallback()throws Exception{
        var source=source();var repository=new JdbcRoomCardPricingProvenanceRepository();
        try(var c=source.getConnection()){
            c.setAutoCommit(false);repository.save(c,snapshot("v1",null,null,null,false));c.rollback();
            assertThrows(IllegalArgumentException.class,()->repository.load(c,1,KEY));assertEquals(0,count(c,"aoo_room_cost_policy_provenance"));
            assertEquals(1,count(c,"aoo_room_cost_publication"));
        }
    }
    @Test void freeWindowsAreExplicitAndInclusiveAndCurrentBindingIsExact(){
        var timed=snapshot("v1","FREE1",FROM,UNTIL,false);
        timed.requireValidAt(FROM,true);timed.requireValidAt(UNTIL,true);timed.requireCurrent(binding());
        assertThrows(IllegalArgumentException.class,()->timed.requireValidAt(FROM.minusMillis(1),true));
        assertThrows(IllegalArgumentException.class,()->timed.requireValidAt(UNTIL.plusMillis(1),true));
        assertThrows(IllegalArgumentException.class,()->timed.requireCurrent(new SourceBinding(SourceClass.CURRENT_PUBLISHED,7,10,"legacy-source-Hash")));
        assertThrows(IllegalArgumentException.class,()->snapshot("v1",null,null,null,false).requireValidAt(FROM,true));
        snapshot("v1",null,null,null,false).requireValidAt(FROM,false);snapshot("v1","FREE_ALWAYS",null,null,true).requireValidAt(FROM,true);
    }
    @Test void invalidSourceTimeAndMetadataAreRejected(){
        assertThrows(IllegalArgumentException.class,()->new RoomCardPricingProvenance(1,KEY,"v1",H,H,new SourceBinding(SourceClass.REGISTERED_ONLY,7,9,"x"),null,null,null,false));
        assertThrows(IllegalArgumentException.class,()->snapshot("v1","FREE",FROM.plusNanos(1),UNTIL,false));
        assertThrows(IllegalArgumentException.class,()->snapshot("v1","FREE",FROM,null,false));
        assertThrows(IllegalArgumentException.class,()->snapshot("v1","FREE",UNTIL,FROM,false));
        assertThrows(IllegalArgumentException.class,()->snapshot("v1","FREE",FROM,UNTIL,true));
        assertThrows(IllegalArgumentException.class,()->snapshot("v1",null,FROM,UNTIL,false));
    }
    @Test void policyKeyUsesExistingStableDimensionsAndNoRequestIdentity(){
        var identity=new IdentityKey(2001,"chengdu2001","pdk-v1","chengdu");
        var key=new PriceKey(identity,"FIXED","HALL",2,"ROUND_COUNT",8,"OWNER",0);
        assertEquals(RoomChargeOperation.hash(2001L,"pdk-v1","chengdu","FIXED","HALL",2,"ROUND_COUNT",8,"OWNER",0L),RoomCardPricingProvenance.policyKey(key));
        assertNotEquals(RoomCardPricingProvenance.policyKey(key),RoomCardPricingProvenance.policyKey(new PriceKey(identity,"FIXED","HALL",2,"ROUND_COUNT",12,"OWNER",0)));
    }
    @Test void missingPublicationForeignKeyErrorRetainsCause()throws Exception{
        var source=source();var repository=new JdbcRoomCardPricingProvenanceRepository();
        var value=new RoomCardPricingProvenance(2,KEY,"v1",H,H,binding(),null,null,null,false);
        try(var c=source.getConnection()){
            c.setAutoCommit(false);var failure=assertThrows(java.sql.SQLException.class,()->repository.save(c,value));
            assertTrue(failure.getSQLState().startsWith("23"));assertEquals(0,count(c,"aoo_room_cost_policy_provenance"));c.rollback();
        }
    }
}
