package com.aoo.bcg.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.Base64;
import java.util.HexFormat;
import static com.aoo.bcg.billing.RoomAaCreation.*;

/** Caller transaction metadata only; the authenticated server source adapter owns authorization. */
public final class JdbcRoomAaCreationRepository {
    private final JdbcRoomChargeCoordinator charges;private final Clock clock;
    private static final ObjectMapper JSON=new ObjectMapper();
    public JdbcRoomAaCreationRepository(JdbcRoomChargeCoordinator charges,Clock clock){this.charges=Objects.requireNonNull(charges);this.clock=Objects.requireNonNull(clock);}
    private Instant now(){return clock.instant().truncatedTo(ChronoUnit.MILLIS);}
    private static void transaction(Connection c)throws SQLException{JdbcRoomChargeCoordinator.requireCallerTransaction(c);}
    public Binding bind(Connection c,Key key,Context context)throws SQLException {
        transaction(c);var old=find(c,key,true);if(old.isPresent()){if(!old.get().context().equals(context))throw new IllegalArgumentException("AA create context conflict");return old.get();}
        String attempt="aac:"+Base64.getUrlEncoder().withoutPadding().encodeToString(HexFormat.of().parseHex(key.sourceKey()));
        var op=new RoomChargeOperation(context.roomId(),attempt,"configuration",key.authenticatedCreatorId(),key.authenticatedCreatorId(),context.gameId(),context.playVersion(),context.regionCode(),RoomChargeOperation.Kind.FIXED,RoomChargeOperation.Context.HALL,RoomChargeOperation.Payer.AA,context.measureKind(),context.measureValue(),context.playerCount(),0);
        var selected=charges.policyForAaCreation(c,op,context.authority());var binding=new Binding(key,context,new PolicyIdentity(context.policyKey(),selected));
        try(var s=c.prepareStatement("INSERT INTO aoo_room_aa_creation(source_key,request_id,request_hash,authenticated_creator_id,room_id,game_id,play_version,region_code,measure_kind,measure_value,player_count,release_id,index_generation,runtime_source_hash,policy_publication_id,policy_key,policy_snapshot,policy_snapshot_hash,binding_hash,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            int i=1;s.setString(i++,key.sourceKey());s.setString(i++,key.requestId());s.setString(i++,key.requestHash());s.setLong(i++,key.authenticatedCreatorId());s.setLong(i++,context.roomId());s.setLong(i++,context.gameId());s.setString(i++,context.playVersion());s.setString(i++,context.regionCode());s.setString(i++,context.measureKind());s.setInt(i++,context.measureValue());s.setInt(i++,context.playerCount());s.setLong(i++,context.authority().releaseId());s.setLong(i++,context.authority().indexGeneration());s.setString(i++,context.authority().runtimeSourceHash());s.setLong(i++,selected.publicationId());s.setString(i++,binding.policy().policyKey());s.setString(i++,selected.json());s.setString(i++,binding.policy().snapshotHash());s.setString(i++,binding.bindingHash());s.setTimestamp(i,Timestamp.from(now()));s.executeUpdate();
        }return binding;
    }
    public Optional<Binding> find(Connection c,Key key,boolean lock)throws SQLException {
        transaction(c);var binding=read(c,"source_key",key.sourceKey(),lock);if(binding!=null&&!binding.key().equals(key))throw new IllegalArgumentException("AA create key identity conflict");return Optional.ofNullable(binding);
    }
    public Optional<Binding> findByRoom(Connection c,long roomId,boolean lock)throws SQLException {
        transaction(c);RoomAaParticipation.positive(roomId);return Optional.ofNullable(read(c,"room_id",roomId,lock));
    }
    private Binding read(Connection c,String column,Object value,boolean lock)throws SQLException {
        if(!Set.of("source_key","room_id").contains(column))throw new IllegalArgumentException("invalid locator");
        try(var s=c.prepareStatement("SELECT * FROM aoo_room_aa_creation WHERE "+column+"=?"+(lock?" FOR UPDATE":""))) {
            s.setObject(1,value);try(var r=s.executeQuery()) {if(!r.next())return null;
                var key=new Key(r.getString("request_id"),r.getString("request_hash"),r.getLong("authenticated_creator_id"));
                if(!key.sourceKey().equals(r.getString("source_key")))throw new IllegalStateException("AA create source checksum");
                var context=new Context(r.getLong("room_id"),r.getLong("game_id"),r.getString("play_version"),r.getString("region_code"),r.getString("measure_kind"),r.getInt("measure_value"),r.getInt("player_count"),new RoomBillingSourceBinding.AuthorityReference(r.getLong("release_id"),r.getLong("index_generation"),r.getString("runtime_source_hash"),r.getString("region_code")));
                var policy=new PolicyIdentity(r.getLong("policy_publication_id"),r.getString("policy_key"),snapshot(r.getString("policy_snapshot")),r.getString("policy_snapshot_hash"));
                return new Binding(key,context,policy,r.getString("binding_hash"));
            }
        }
    }
    private static JdbcRoomCostPolicyRepository.PolicySnapshot snapshot(String text) {
        try{var n=JSON.readTree(text);var fields=new HashSet<String>();n.fieldNames().forEachRemaining(fields::add);
            if(!fields.equals(Set.of("publicationId","policyVersion","contentHash","unitCost","amount","currency","freeRef","refundRef","tierMinor")))throw new IllegalArgumentException("AA policy snapshot fields");
            for(String f:List.of("publicationId","policyVersion","unitCost","amount","tierMinor"))if(!n.get(f).isIntegralNumber()||!n.get(f).canConvertToLong())throw new IllegalArgumentException("AA policy integer");
            for(String f:List.of("contentHash","currency","refundRef"))if(!n.get(f).isTextual())throw new IllegalArgumentException("AA policy token");
            if(!n.get("freeRef").isNull()&&!n.get("freeRef").isTextual())throw new IllegalArgumentException("AA free reference");
            return new JdbcRoomCostPolicyRepository.PolicySnapshot(n.get("publicationId").longValue(),n.get("policyVersion").longValue(),n.get("contentHash").textValue(),n.get("unitCost").longValue(),n.get("amount").longValue(),n.get("currency").textValue(),n.get("freeRef").isNull()?null:n.get("freeRef").textValue(),n.get("refundRef").textValue(),n.get("tierMinor").longValue());
        }catch(java.io.IOException invalid){throw new IllegalArgumentException("invalid AA stored policy",invalid);}
    }
    private Binding matching(Connection c,Binding binding)throws SQLException {
        var actual=find(c,binding.key(),true).orElseThrow(()->new IllegalArgumentException("AA creation source missing"));if(!actual.equals(binding))throw new IllegalArgumentException("AA creation binding mismatch");return actual;
    }
    public Optional<Receipt> outcome(Connection c,Binding binding,EventType type)throws SQLException {
        transaction(c);matching(c,binding);try(var s=c.prepareStatement("SELECT * FROM aoo_room_aa_creation_outcome WHERE source_key=? AND event_type=? FOR UPDATE")){s.setString(1,binding.key().sourceKey());s.setString(2,type.name());try(var r=s.executeQuery()){
            if(!r.next())return Optional.empty();RuntimeProof proof=type==EventType.ABORTED_BEFORE_COMMIT?null:new RuntimeProof(r.getString("room_instance_id"),r.getLong("lifecycle_epoch"),r.getLong("fencing_token"),r.getLong("state_version"),r.getLong("creator_id"),r.getLong("game_id"),r.getString("play_version"));
            if(proof!=null)context(binding,proof);var receipt=new Receipt(type,proof,r.getTimestamp("created_at").toInstant());if(!receipt.hash().equals(r.getString("outcome_hash")))throw new IllegalStateException("AA outcome checksum");return Optional.of(receipt);
        }}}
    private static void context(Binding b,RuntimeProof p){if(p.creatorId()!=b.key().authenticatedCreatorId()||p.gameId()!=b.context().gameId()||!p.playVersion().equals(b.context().playVersion()))throw new IllegalArgumentException("AA runtime context mismatch");}
    private static void identity(RuntimeProof original,RuntimeProof current){if(!original.roomInstanceId().equals(current.roomInstanceId())||original.lifecycleEpoch()!=current.lifecycleEpoch()||original.fencingToken()!=current.fencingToken()||original.creatorId()!=current.creatorId()||original.gameId()!=current.gameId()||!original.playVersion().equals(current.playVersion())||current.stateVersion()<original.stateVersion())throw new IllegalArgumentException("AA runtime identity mismatch");}
    /** Conditional DML is the current-state authority, including under an established RR snapshot. */
    private void requireState(Connection c,Binding b,String state)throws SQLException {
        try(var s=c.prepareStatement("UPDATE aoo_room_aa_creation SET lifecycle_state=lifecycle_state WHERE source_key=? AND binding_hash=? AND lifecycle_state=?")) {
            s.setString(1,b.key().sourceKey());s.setString(2,b.bindingHash());s.setString(3,state);
            if(s.executeUpdate()!=1)throw new IllegalStateException("AA creation current lifecycle state conflict");
        }
    }
    private boolean transition(Connection c,Binding b,String expected,String target)throws SQLException {
        try(var s=c.prepareStatement("UPDATE aoo_room_aa_creation SET lifecycle_state=? WHERE source_key=? AND binding_hash=? AND lifecycle_state=?")) {
            s.setString(1,target);s.setString(2,b.key().sourceKey());s.setString(3,b.bindingHash());s.setString(4,expected);
            if(s.executeUpdate()==1)return true;
        }
        requireState(c,b,target);
        return false;
    }
    public Receipt recordCommit(Connection c,Binding b,RuntimeProof proof)throws SQLException {
        transaction(c);matching(c,b);context(b,proof);boolean first=transition(c,b,"PENDING","COMMITTED");return write(c,b,new Receipt(EventType.COMMITTED,proof,now()),first);
    }
    public Receipt requireCommitted(Connection c,Binding b,RuntimeProof proof)throws SQLException {
        transaction(c);matching(c,b);context(b,proof);requireState(c,b,"COMMITTED");var receipt=outcome(c,b,EventType.COMMITTED).orElseThrow(()->new IllegalStateException("AA create UNKNOWN not committed"));identity(receipt.proof(),proof);return receipt;
    }
    public Receipt abortBeforeCommit(Connection c,Binding b)throws SQLException {
        transaction(c);matching(c,b);boolean first=transition(c,b,"PENDING","ABORTED_BEFORE_COMMIT");return write(c,b,new Receipt(EventType.ABORTED_BEFORE_COMMIT,null,now()),first);
    }
    public Receipt recordDestroy(Connection c,Binding b,RuntimeProof proof)throws SQLException {
        transaction(c);matching(c,b);context(b,proof);var original=outcome(c,b,EventType.COMMITTED).orElseThrow(()->new IllegalStateException("AA destroy lacks commit"));identity(original.proof(),proof);boolean first=transition(c,b,"COMMITTED","DESTROYED");return write(c,b,new Receipt(EventType.DESTROYED,proof,now()),first);
    }
    private Receipt write(Connection c,Binding b,Receipt receipt,boolean first)throws SQLException {
        var old=outcome(c,b,receipt.eventType());if(old.isPresent()){if(first)throw new IllegalStateException("AA lifecycle receipt precedes transition");if(!old.get().hash().equals(receipt.hash()))throw new IllegalArgumentException("immutable AA outcome conflict");return old.get();}
        if(!first)throw new IllegalStateException("AA lifecycle target without receipt");
        try(var s=c.prepareStatement("INSERT INTO aoo_room_aa_creation_outcome(source_key,event_type,room_instance_id,lifecycle_epoch,fencing_token,state_version,creator_id,game_id,play_version,outcome_hash,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)")) {
            s.setString(1,b.key().sourceKey());s.setString(2,receipt.eventType().name());var p=receipt.proof();s.setString(3,p==null?null:p.roomInstanceId());s.setObject(4,p==null?null:p.lifecycleEpoch(),Types.BIGINT);s.setObject(5,p==null?null:p.fencingToken(),Types.BIGINT);s.setObject(6,p==null?null:p.stateVersion(),Types.BIGINT);s.setObject(7,p==null?null:p.creatorId(),Types.BIGINT);s.setObject(8,p==null?null:p.gameId(),Types.BIGINT);s.setString(9,p==null?null:p.playVersion());s.setString(10,receipt.hash());s.setTimestamp(11,Timestamp.from(receipt.createdAt()));s.executeUpdate();
        }return receipt;
    }
    Binding requireParticipation(Connection c,RoomAaParticipation.SessionKey key,RoomAaParticipation.Context context,RoomAaParticipation.PreSeatState pre)throws SQLException {
        var binding=findByRoom(c,key.roomId(),true).orElseThrow(()->new IllegalStateException("AA creation source missing"));
        if(!binding.context().participation(binding.key().authenticatedCreatorId()).equals(context))throw new IllegalArgumentException("AA participant creation context conflict");
        requireCommitted(c,binding,new RuntimeProof(key.roomInstanceId(),key.lifecycleEpoch(),pre.fencingToken(),pre.stateVersion(),context.creatorId(),context.gameId(),context.playVersion()));return binding;
    }
}
