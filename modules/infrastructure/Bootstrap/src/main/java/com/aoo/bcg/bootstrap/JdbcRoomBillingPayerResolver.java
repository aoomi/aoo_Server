package com.aoo.bcg.bootstrap;

import com.aoo.bcg.billing.RoomChargeOperation;
import com.aoo.bcg.billing.JdbcRoomChargeCoordinator;
import com.aoo.bcg.billing.JdbcBillingService;
import com.aoo.bcg.billing.JdbcRoomCostPolicyRepository;
import javax.sql.DataSource;
import java.time.Clock;
import com.aoo.bcg.club.JdbcClubService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;

/** Resolves only server-owned room subjects; never commits or charges funds. */
public final class JdbcRoomBillingPayerResolver implements JdbcRoomChargeCoordinator.TrustedPayerVerifier {
    public record Subject(long initiatingActorId,long creatorId,long sourceClubId) {
        public Subject { if(initiatingActorId<=0||creatorId<=0||sourceClubId<0)throw new IllegalArgumentException("invalid server room subject"); }
    }
    private final ObjectMapper json;
    public JdbcRoomBillingPayerResolver(ObjectMapper json){this.json=Objects.requireNonNull(json);}
    @Override public void verify(Connection c,RoomChargeOperation operation,JdbcRoomChargeCoordinator.Subject subject)throws SQLException{
        Objects.requireNonNull(subject);
        verify(c,operation,new Subject(subject.initiatingActorId(),subject.creatorId(),subject.sourceClubId()));
    }
    public JdbcRoomChargeCoordinator createCoordinator(DataSource source,JdbcBillingService billing,
            JdbcRoomCostPolicyRepository policies,Clock clock){
        return new JdbcRoomChargeCoordinator(source,billing,policies,clock,this);
    }
    public void verify(Connection c,RoomChargeOperation operation,Subject subject)throws SQLException{
        if(resolve(c,operation,subject)!=operation.payerId())throw new SecurityException("trusted payer mismatch");
    }
    public long resolve(Connection c,RoomChargeOperation operation,Subject subject)throws SQLException{
        Objects.requireNonNull(c);Objects.requireNonNull(operation);Objects.requireNonNull(subject);
        if(c.isClosed()||c.getAutoCommit()||c.isReadOnly())throw new SQLException("caller write transaction required");
        if(operation.context()==RoomChargeOperation.Context.HALL){
            if(subject.sourceClubId()!=0)throw new SecurityException("hall subject has organization");
            if(operation.payerMode()==RoomChargeOperation.Payer.OWNER&&operation.actorId()!=subject.creatorId())throw new SecurityException("hall owner must be creator");
            return operation.actorId();
        }
        if(subject.sourceClubId()<=0)throw new SecurityException("organization required");
        var source=load(c,subject.sourceClubId());
        if(!source.members().containsKey(subject.initiatingActorId()))throw new SecurityException("active organization member required");
        long owner=owner(source);
        if(operation.context()==RoomChargeOperation.Context.CLUB)return owner;
        long union=setting(source,"unionId"),ownerClubId=setting(source,"unionOwnerClubId"),unionOwner=setting(source,"unionOwnerId");
        var ownerClub=ownerClubId==source.id()?source:load(c,ownerClubId);
        long actualOwner=owner(ownerClub);
        if(setting(ownerClub,"unionId")!=union||setting(ownerClub,"unionOwnerClubId")!=ownerClub.id()
                ||setting(ownerClub,"unionOwnerId")!=actualOwner||unionOwner!=actualOwner)throw new SecurityException("union ownership mismatch");
        return actualOwner;
    }
    private JdbcClubService.State load(Connection c,long id)throws SQLException{
        try(var query=c.prepareStatement("SELECT state_json FROM aoo_club_state WHERE club_id=? FOR UPDATE")){
            query.setLong(1,id);query.setQueryTimeout(3);
            try(var rows=query.executeQuery()){
                if(!rows.next())throw new SecurityException("organization missing");
                final JdbcClubService.State state;
                try{state=json.readValue(rows.getString(1),JdbcClubService.State.class);}catch(Exception invalid){throw new SQLException("invalid organization state");}
                if(rows.next()||state.id()!=id||!"ACTIVE".equals(state.status()))throw new SecurityException("organization inactive or identity mismatch");
                return state;
            }
        }
    }
    private static long owner(JdbcClubService.State state){
        long found=0;int count=0;
        for(var member:state.members().entrySet())if("OWNER".equals(member.getValue())){
            if(member.getKey()==null||member.getKey()<=0)throw new SecurityException("invalid owner");
            found=member.getKey();count++;
        }
        if(count!=1)throw new SecurityException("unique owner required");
        return found;
    }
    private static long setting(JdbcClubService.State state,String key){
        Object value=state.settings().get(key);
        if(!(value instanceof Number number))throw new SecurityException("explicit numeric union relationship required");
        try{long result=new BigDecimal(number.toString()).longValueExact();if(result<=0)throw new ArithmeticException();return result;}
        catch(ArithmeticException|NumberFormatException invalid){throw new SecurityException("invalid union relationship");}
    }
}
