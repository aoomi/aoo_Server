package com.aoo.bcg.billing;

import java.sql.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import static com.aoo.bcg.billing.RoomAaParticipation.*;

/** Called before native mutation and after actual persisted seat proof, within one caller transaction. */
public final class JdbcRoomAaParticipationService {
    public interface TrustedSourceVerifier {
        PreSeatState verifyBefore(Connection c,SessionKey key,Context context,CommandKey command)throws SQLException;
        void verifyPersistedSeat(Connection c,SessionKey key,Context context,CommandKey command,SeatProof proof)throws SQLException;
    }
    private final JdbcRoomChargeCoordinator charges;
    private final Clock clock;
    private final TrustedSourceVerifier verifier;
    private final JdbcRoomAaCreationRepository creations;
    private final JdbcRoomAaParticipationRepository repository=new JdbcRoomAaParticipationRepository();
    private final JdbcRoomBillingIntentJournal journal=new JdbcRoomBillingIntentJournal();
    public JdbcRoomAaParticipationService(JdbcRoomChargeCoordinator charges,Clock clock,TrustedSourceVerifier verifier) {
        this.charges=Objects.requireNonNull(charges);this.clock=Objects.requireNonNull(clock);this.verifier=Objects.requireNonNull(verifier);this.creations=new JdbcRoomAaCreationRepository(charges,clock);
    }
    private Instant now(){return clock.instant().truncatedTo(ChronoUnit.MILLIS);}
    private PreSeatState verify(Connection c,SessionKey key,Context context,CommandKey command)throws SQLException {
        JdbcRoomChargeCoordinator.requireCallerTransaction(c);
        try{return Objects.requireNonNull(verifier.verifyBefore(c,key,context,command));}
        catch(SQLException denied){throw new IllegalStateException("AA trusted source denied",denied);}
    }
    public Preparation prepare(Connection c,SessionKey key,Context context,CommandKey command)throws SQLException {
        var pre=verify(c,key,context,command);var creation=creations.requireParticipation(c,key,context,pre);var at=now();long session=repository.session(c,key,context,at,true);
        var existing=repository.command(c,session,key,command);
        if(existing!=null) {
            if(existing.receipt()==null)throw new IllegalStateException("UNKNOWN AA pending command");
            return preparation(c,key,context,command,session,existing.attempt(),existing.pre(),Disposition.COMMAND_REPLAY,existing.receipt(),existing.hash());
        }
        var latest=repository.attempt(c,session,command.authenticatedActorId(),null);long ordinal=1;
        if(latest!=null) {
            if(!"SEATED".equals(latest.state()))throw new IllegalStateException("UNKNOWN AA pending attempt");
            var old=charges.findForAa(c,"rc:"+latest.operationKey());if(old==null)throw new IllegalStateException("UNKNOWN AA original charge");
            if("RESERVED".equals(old.state())||"CONFIRMED".equals(old.state())) {
                var p=preparation(c,key,context,command,session,latest.ordinal(),pre,Disposition.REUSE_PAID,null,null);
                repository.insertCommand(c,p,at);return p;
            }
            if(!"RELEASED".equals(old.state())||old.refundedMinor()!=old.amount())throw new IllegalStateException("invalid AA refund state");
            ordinal=Math.addExact(latest.ordinal(),1);
        }
        if(pre.actorSeatBefore()!=-1)throw new IllegalArgumentException("new AA attempt requires actual spectator");
        long actor=command.authenticatedActorId();var operation=new RoomChargeOperation(key.roomId(),key.roomAttempt(),"p:"+actor+":a:"+ordinal,actor,actor,context.gameId(),context.playVersion(),context.regionCode(),
                RoomChargeOperation.Kind.FIXED,RoomChargeOperation.Context.HALL,RoomChargeOperation.Payer.AA,context.measureKind(),context.measureValue(),context.playerCount(),0);
        var intent=new RoomBillingIntent(operation,at);var subject=new JdbcRoomChargeCoordinator.Subject(actor,context.creatorId(),0);
        journal.append(c,intent);String binding=binding(key,context,actor,ordinal,intent,subject);
        repository.insertAttempt(c,session,actor,ordinal,intent,subject.commandHash(operation),binding,at);
        var charge=charges.reserveAa(c,operation,subject,context.authority(),creation.policy());
        var p=new Preparation(key,context,command,session,ordinal,intent,subject,charge,pre,Disposition.NEW_ATTEMPT,
                RoomAaParticipation.preparation(key,context,command,ordinal,binding,pre),null);
        repository.insertCommand(c,p,at);return p;
    }
    private Preparation preparation(Connection c,SessionKey key,Context context,CommandKey command,long session,long ordinal,PreSeatState pre,Disposition disposition,SeatReceipt replay,String expectedHash)throws SQLException {
        var a=repository.attempt(c,session,command.authenticatedActorId(),ordinal);if(a==null)throw new IllegalStateException("AA original attempt missing");
        var entry=journal.find(c,a.operationKey());if(entry==null)throw new IllegalStateException("AA original intent missing");
        var intent=entry.intent();var op=intent.operation();var subject=new JdbcRoomChargeCoordinator.Subject(command.authenticatedActorId(),context.creatorId(),0);
        if(!a.subjectHash().equals(subject.commandHash(op))||!a.bindingHash().equals(binding(key,context,command.authenticatedActorId(),ordinal,intent,subject)))throw new IllegalArgumentException("AA attempt binding conflict");
        var charge=charges.findForAa(c,op.requestId());if(charge==null||!charge.commandHash().equals(a.subjectHash()))throw new IllegalStateException("AA original charge binding conflict");
        String hash=RoomAaParticipation.preparation(key,context,command,ordinal,a.bindingHash(),pre);if(expectedHash!=null&&!expectedHash.equals(hash))throw new IllegalArgumentException("AA preparation hash conflict");
        return new Preparation(key,context,command,session,ordinal,intent,subject,charge,pre,disposition,hash,replay);
    }
    public Optional<SeatReceipt> findReceipt(Connection c,SessionKey key,Context context,CommandKey command)throws SQLException {
        var pre=verify(c,key,context,command);creations.requireParticipation(c,key,context,pre);long session=repository.session(c,key,context,now(),false);if(session==0)return Optional.empty();
        var row=repository.command(c,session,key,command);if(row==null)return Optional.empty();if(row.receipt()==null)throw new IllegalStateException("UNKNOWN AA pending command");
        preparation(c,key,context,command,session,row.attempt(),row.pre(),Disposition.COMMAND_REPLAY,row.receipt(),row.hash());return Optional.of(row.receipt());
    }
    public SeatReceipt commitSeated(Connection c,Preparation p,SeatProof proof)throws SQLException {
        JdbcRoomChargeCoordinator.requireCallerTransaction(c);Objects.requireNonNull(p);Objects.requireNonNull(proof);
        creations.requireParticipation(c,p.key(),p.context(),p.preState());
        long session=repository.session(c,p.key(),p.context(),now(),false);if(session!=p.sessionId())throw new IllegalArgumentException("AA session conflict");
        var row=repository.command(c,session,p.key(),p.command());if(row==null)throw new IllegalStateException("AA preparation missing");
        var stored=preparation(c,p.key(),p.context(),p.command(),session,row.attempt(),row.pre(),p.disposition(),row.receipt(),row.hash());
        if(!stored.equals(p))throw new IllegalArgumentException("AA forged preparation");
        if(!proof.requestId().equals(p.command().requestId())||!proof.requestHash().equals(p.command().requestHash())||proof.actorId()!=p.command().authenticatedActorId()
                ||!proof.roomInstanceId().equals(p.key().roomInstanceId())||proof.lifecycleEpoch()!=p.key().lifecycleEpoch()||proof.seatId()>=p.context().playerCount()
                ||proof.fencingToken()!=row.pre().fencingToken()||proof.stateVersion()<=row.pre().stateVersion())throw new IllegalArgumentException("AA seat proof identity conflict");
        try{verifier.verifyPersistedSeat(c,p.key(),p.context(),p.command(),proof);}catch(SQLException denied){throw new IllegalStateException("AA persisted seat denied",denied);}
        if(row.receipt()!=null){if(!row.receipt().proof().equals(proof))throw new IllegalArgumentException("AA receipt conflict");return row.receipt();}
        repository.commit(c,p,proof,now());return repository.command(c,session,p.key(),p.command()).receipt();
    }
}
