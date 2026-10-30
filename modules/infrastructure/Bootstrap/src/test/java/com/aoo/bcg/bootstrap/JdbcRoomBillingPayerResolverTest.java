package com.aoo.bcg.bootstrap;

import com.aoo.bcg.billing.RoomChargeOperation;
import com.aoo.bcg.billing.JdbcRoomChargeCoordinator;
import com.aoo.bcg.billing.JdbcBillingService;
import com.aoo.bcg.billing.JdbcRoomCostPolicyRepository;
import org.h2.jdbcx.JdbcDataSource;
import java.time.Clock;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

final class JdbcRoomBillingPayerResolverTest {
    private final ObjectMapper json=new ObjectMapper();
    private final JdbcRoomBillingPayerResolver resolver=new JdbcRoomBillingPayerResolver(json);
    private String url;
    @BeforeEach void setup()throws Exception {
        url="jdbc:h2:mem:payer"+UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=100";
        try(var c=DriverManager.getConnection(url)){c.createStatement().execute("CREATE TABLE aoo_club_state(club_id BIGINT PRIMARY KEY,state_json VARCHAR(16000) NOT NULL)");}
    }
    private void state(long id,String status,Map<Long,String> members,Map<String,Object> settings)throws Exception{
        try(var c=DriverManager.getConnection(url);var q=c.prepareStatement("INSERT INTO aoo_club_state VALUES(?,?)")){
            q.setLong(1,id);q.setString(2,json.writeValueAsString(Map.of("id",id,"name","fixture","status",status,"members",members,"settings",settings)));q.executeUpdate();
        }
    }
    private Connection tx()throws SQLException{var c=DriverManager.getConnection(url);c.setAutoCommit(false);return c;}
    private RoomChargeOperation operation(RoomChargeOperation.Context context,long actor,long payer,boolean carry){
        var mode=switch(context){case HALL->RoomChargeOperation.Payer.OWNER;case CLUB->RoomChargeOperation.Payer.CLUB;case UNION->RoomChargeOperation.Payer.UNION;};
        return new RoomChargeOperation(100,"attempt","unit",actor,payer,2001,"fixture-v1","fixture",carry?RoomChargeOperation.Kind.CARRY:RoomChargeOperation.Kind.FIXED,context,mode,carry?"CARRY_MINOR_PER_TIER":"ROUND_COUNT",8,2,carry?100:0);
    }
    private JdbcRoomBillingPayerResolver.Subject subject(long club){return new JdbcRoomBillingPayerResolver.Subject(10,40,club);}
    @Test void hallCreatorAndAaAreChargingSubjects()throws Exception{
        try(var c=tx()){
            resolver.verify(c,operation(RoomChargeOperation.Context.HALL,40,40,false),subject(0));
            var aa=new RoomChargeOperation(100,"attempt","unit",50,50,2001,"fixture-v1","fixture",RoomChargeOperation.Kind.FIXED,RoomChargeOperation.Context.HALL,RoomChargeOperation.Payer.AA,"ROUND_COUNT",8,2,0);
            resolver.verify(c,aa,subject(0));assertFalse(c.getAutoCommit());
            assertThrows(SecurityException.class,()->resolver.verify(c,operation(RoomChargeOperation.Context.HALL,10,10,false),subject(0)));
            assertThrows(SecurityException.class,()->resolver.verify(c,operation(RoomChargeOperation.Context.HALL,40,40,false),subject(100)));
        }
    }
    @Test void normalMemberOrganizationCarryChargesOwnerNotInitiatorOrCreator()throws Exception{
        state(100,"ACTIVE",Map.of(10L,"MEMBER",20L,"OWNER"),Map.of());
        try(var c=tx()){
            resolver.verify(c,operation(RoomChargeOperation.Context.CLUB,40,20,true),subject(100));
            assertThrows(SecurityException.class,()->resolver.verify(c,operation(RoomChargeOperation.Context.CLUB,40,10,true),subject(100)));
            assertThrows(SecurityException.class,()->resolver.verify(c,operation(RoomChargeOperation.Context.CLUB,40,20,false),new JdbcRoomBillingPayerResolver.Subject(99,40,100)));
        }
    }
    @Test void unionLocksAndCrossChecksExplicitCurrentOwner()throws Exception{
        state(100,"ACTIVE",Map.of(10L,"MEMBER",20L,"OWNER"),Map.of("unionId",500,"unionOwnerClubId",200,"unionOwnerId",30));
        state(200,"ACTIVE",Map.of(30L,"OWNER"),Map.of("unionId",500,"unionOwnerClubId",200,"unionOwnerId",30));
        try(var c=tx()){
            resolver.verify(c,operation(RoomChargeOperation.Context.UNION,40,30,true),subject(100));
            assertThrows(SecurityException.class,()->resolver.verify(c,operation(RoomChargeOperation.Context.UNION,40,20,false),subject(100)));
        }
    }
    @Test void invalidStateAndOwnerNeverFallback()throws Exception{
        state(100,"RETIRED",Map.of(10L,"MEMBER",20L,"OWNER"),Map.of());
        state(101,"ACTIVE",Map.of(10L,"MEMBER"),Map.of());
        state(102,"ACTIVE",Map.of(10L,"MEMBER",20L,"OWNER",30L,"OWNER"),Map.of());
        try(var c=tx()){for(long id:new long[]{100,101,102,103})assertThrows(SecurityException.class,()->resolver.resolve(c,operation(RoomChargeOperation.Context.CLUB,40,20,false),subject(id)));}
    }
    @Test void missingAndMalformedUnionFieldsReject()throws Exception{
        List<Map<String,Object>> settings=List.of(Map.of(),Map.of("unionId","500","unionOwnerClubId",200,"unionOwnerId",30),Map.of("unionId",500.5,"unionOwnerClubId",200,"unionOwnerId",30),Map.of("unionId",500,"unionOwnerClubId",0,"unionOwnerId",30));
        for(int i=0;i<settings.size();i++)state(100+i,"ACTIVE",Map.of(10L,"MEMBER",20L,"OWNER"),settings.get(i));
        try(var c=tx()){for(long id=100;id<104;id++){long club=id;assertThrows(SecurityException.class,()->resolver.resolve(c,operation(RoomChargeOperation.Context.UNION,40,30,false),subject(club)));}}
    }
    @Test void unionDriftAndOwnerChangeReject()throws Exception{
        state(100,"ACTIVE",Map.of(10L,"MEMBER",20L,"OWNER"),Map.of("unionId",500,"unionOwnerClubId",200,"unionOwnerId",30));
        state(200,"ACTIVE",Map.of(31L,"OWNER"),Map.of("unionId",500,"unionOwnerClubId",200,"unionOwnerId",30));
        try(var c=tx()){assertThrows(SecurityException.class,()->resolver.resolve(c,operation(RoomChargeOperation.Context.UNION,40,30,false),subject(100)));}
    }
    @Test void explicitCallerTransactionRequired()throws Exception{
        try(var c=DriverManager.getConnection(url)){assertThrows(SQLException.class,()->resolver.resolve(c,operation(RoomChargeOperation.Context.HALL,40,40,false),subject(0)));}
    }
    @Test void rowLocksRemainUntilCallerRollback()throws Exception{
        state(100,"ACTIVE",Map.of(10L,"MEMBER",20L,"OWNER"),Map.of());
        try(var owner=tx();var contender=DriverManager.getConnection(url)){
            resolver.verify(owner,operation(RoomChargeOperation.Context.CLUB,40,20,false),subject(100));
            assertFalse(owner.getAutoCommit());
            try(var update=contender.prepareStatement("UPDATE aoo_club_state SET state_json=state_json WHERE club_id=100")){
                assertThrows(SQLException.class,update::executeUpdate);owner.rollback();assertEquals(1,update.executeUpdate());
            }
        }
    }
    @Test void readonlyConnectionRefusedWithoutQuery()throws Exception{
        var readonly=(Connection)java.lang.reflect.Proxy.newProxyInstance(getClass().getClassLoader(),new Class<?>[]{Connection.class},(proxy,method,args)->switch(method.getName()){case "isClosed","getAutoCommit"->false;case "isReadOnly"->true;default->throw new AssertionError("unexpected connection call");});
        assertThrows(SQLException.class,()->resolver.resolve(readonly,operation(RoomChargeOperation.Context.HALL,40,40,false),subject(0)));
    }
    @Test void unionOwnerClubMustExistAndShareExactUnion()throws Exception{
        state(100,"ACTIVE",Map.of(10L,"MEMBER",20L,"OWNER"),Map.of("unionId",500,"unionOwnerClubId",200,"unionOwnerId",30));
        state(101,"ACTIVE",Map.of(10L,"MEMBER",20L,"OWNER"),Map.of("unionId",500,"unionOwnerClubId",999,"unionOwnerId",30));
        state(200,"ACTIVE",Map.of(30L,"OWNER"),Map.of("unionId",501,"unionOwnerClubId",200,"unionOwnerId",30));
        try(var c=tx()){
            assertThrows(SecurityException.class,()->resolver.resolve(c,operation(RoomChargeOperation.Context.UNION,40,30,false),subject(100)));
            assertThrows(SecurityException.class,()->resolver.resolve(c,operation(RoomChargeOperation.Context.UNION,40,30,false),subject(101)));
        }
    }
    @Test void unionOwnerRowAlsoLockedUntilCallerRollback()throws Exception{
        state(100,"ACTIVE",Map.of(10L,"MEMBER",20L,"OWNER"),Map.of("unionId",500,"unionOwnerClubId",200,"unionOwnerId",30));
        state(200,"ACTIVE",Map.of(30L,"OWNER"),Map.of("unionId",500,"unionOwnerClubId",200,"unionOwnerId",30));
        try(var owner=tx();var contender=DriverManager.getConnection(url)){
            resolver.verify(owner,operation(RoomChargeOperation.Context.UNION,40,30,false),subject(100));
            try(var update=contender.prepareStatement("UPDATE aoo_club_state SET state_json=state_json WHERE club_id=200")){
                assertThrows(SQLException.class,update::executeUpdate);owner.rollback();assertEquals(1,update.executeUpdate());
            }
        }
    }
    private JdbcRoomChargeCoordinator coordinator(){
        var source=new JdbcDataSource();source.setURL(url);
        return resolver.createCoordinator(source,new JdbcBillingService(source,()->1000L,Clock.systemUTC()),
                new JdbcRoomCostPolicyRepository(source,false),Clock.systemUTC());
    }
    private void originalReceipt(RoomChargeOperation operation)throws Exception{
        try(var c=DriverManager.getConnection(url)){
            c.createStatement().execute("CREATE TABLE aoo_room_billing_reservation(request_id VARCHAR(128) PRIMARY KEY,command_hash VARCHAR(64),account_id BIGINT,room_id BIGINT,currency_code VARCHAR(32),amount BIGINT,refunded_minor BIGINT,state VARCHAR(32),policy_publication_id BIGINT,policy_snapshot VARCHAR(1024),confirmation_id VARCHAR(128),confirmation_hash VARCHAR(64))");
            try(var q=c.prepareStatement("INSERT INTO aoo_room_billing_reservation VALUES(?,?,?,?,?,7,0,'RESERVED',1,'{}',NULL,NULL)")){
                q.setString(1,operation.requestId());q.setString(2,operation.commandHash());q.setLong(3,operation.payerId());q.setLong(4,operation.roomId());q.setString(5,"ROOM_CARD");q.executeUpdate();
            }
        }
    }
    @Test void realCoordinatorFactoryReplaysReceiptAndRevalidatesOwner()throws Exception{
        state(100,"ACTIVE",Map.of(10L,"MEMBER",20L,"OWNER"),Map.of());
        var operation=operation(RoomChargeOperation.Context.CLUB,40,20,false);originalReceipt(operation);
        var coordinator=coordinator();var subject=new JdbcRoomChargeCoordinator.Subject(10,40,100);
        var charge=coordinator.reserve(operation,subject);assertEquals(20,charge.payerId());assertEquals(7,charge.amount());
        try(var c=DriverManager.getConnection(url);var q=c.prepareStatement("UPDATE aoo_club_state SET state_json=? WHERE club_id=100")){
            q.setString(1,json.writeValueAsString(Map.of("id",100,"name","fixture","status","ACTIVE","members",Map.of(10L,"MEMBER",30L,"OWNER"),"settings",Map.of())));q.executeUpdate();
        }
        assertThrows(SecurityException.class,()->coordinator.reserve(operation,subject));
        try(var c=DriverManager.getConnection(url);var q=c.createStatement();var rows=q.executeQuery("SELECT amount,account_id FROM aoo_room_billing_reservation")){
            assertTrue(rows.next());assertEquals(7,rows.getLong(1));assertEquals(20,rows.getLong(2));assertFalse(rows.next());
        }
    }
    @Test void realCoordinatorUsesTypedInitiatorMembership()throws Exception{
        state(100,"ACTIVE",Map.of(10L,"MEMBER",20L,"OWNER"),Map.of());
        var operation=operation(RoomChargeOperation.Context.CLUB,40,20,true);originalReceipt(operation);
        assertThrows(SecurityException.class,()->coordinator().reserve(operation,new JdbcRoomChargeCoordinator.Subject(99,40,100)));
        assertEquals(20,coordinator().reserve(operation,new JdbcRoomChargeCoordinator.Subject(10,40,100)).payerId());
    }
}
