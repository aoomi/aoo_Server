package com.aoo.bcg.billing;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Deterministic reserve/consume/release/refund coordinator for every room payment mode.
 * Its immutable view is the persistence boundary: production callers must store each returned version.
 */
public final class RoomChargeCoordinator {
    private final BillingService billing;
    private final Clock clock;
    private final Map<Long,RoomState> rooms=new LinkedHashMap<>();

    public RoomChargeCoordinator(BillingService billing,Clock clock){
        this.billing=Objects.requireNonNull(billing,"billing");
        this.clock=Objects.requireNonNull(clock,"clock");
    }

    public synchronized RoomBillingView open(RoomChargeCommand command){
        Objects.requireNonNull(command,"command");
        RoomState existing=rooms.get(command.roomId());
        if(existing!=null){existing.requireSame(command);return existing.view();}
        validate(command);
        RoomState state=new RoomState(command,clock.instant());
        try{
            for(var item:command.charges().entrySet())reserve(state,item.getKey(),item.getValue());
            rooms.put(command.roomId(),state);
            return state.view();
        }catch(RuntimeException failure){
            state.failure="RESERVATION_FAILED:"+failure.getClass().getSimpleName();
            compensate(state,true);
            if(!state.charges.isEmpty())rooms.put(command.roomId(),state);
            if(state.status==RoomStatus.COMPENSATION_PENDING)return state.view();
            throw failure;
        }
    }

    /** Adds an AA participant before charging; insufficient players remain explicit and cleanable. */
    public synchronized RoomBillingView joinAa(long roomId,CurrencyAccount account){
        RoomState state=state(roomId);
        if(state.policy.paymentSubject()!=BillingPolicySnapshot.PaymentSubject.AA)
            throw new IllegalStateException("room is not AA");
        if(state.status!=RoomStatus.PROVISIONING)throw new IllegalStateException("room no longer accepts payers");
        if(state.charges.containsKey(account.playerId()))return state.view();
        reserve(state,account,state.policy.amountForAaPlayer());
        state.version++;
        return state.view();
    }

    public synchronized RoomBillingView leaveAa(long roomId,long playerId,String reason){
        RoomState state=state(roomId);requireReason(reason);
        Charge charge=charge(state,playerId);
        if(charge.status==ChargeStatus.RESERVED)release(state,charge,"AA_LEAVE."+reason);
        else if(charge.status==ChargeStatus.CONSUMED)throw new IllegalStateException("charged player cannot leave");
        state.version++;
        return state.view();
    }

    public synchronized boolean ready(long roomId){
        RoomState state=state(roomId);
        long available=state.charges.values().stream().filter(c->c.status==ChargeStatus.RESERVED||c.status==ChargeStatus.CONSUMED).count();
        return available>=state.policy.minimumPlayers();
    }

    public synchronized RoomBillingView provisioned(long roomId,ProvisionStep step){
        RoomState state=state(roomId);Objects.requireNonNull(step,"step");
        if(state.status!=RoomStatus.PROVISIONING)throw new IllegalStateException("room is not provisioning");
        if(step.ordinal()>state.provisionStep.ordinal()+1)throw new IllegalStateException("provision step skipped");
        if(step.ordinal()>state.provisionStep.ordinal()){state.provisionStep=step;state.version++;}
        return state.view();
    }

    /** Locks the unique charge instant from the server-authored policy. */
    public synchronized RoomBillingView chargeAt(long roomId,BillingPolicySnapshot.ChargePoint actualPoint){
        RoomState state=state(roomId);Objects.requireNonNull(actualPoint,"actualPoint");
        if(actualPoint!=state.policy.chargePoint())throw new IllegalStateException("wrong charge point");
        if(state.provisionStep!=ProvisionStep.TABLE_PUSHED)throw new IllegalStateException("room provisioning incomplete");
        for(Charge charge:state.charges.values())if(charge.status==ChargeStatus.RESERVED)charge.status=ChargeStatus.CONSUMED;
        state.status=actualPoint==BillingPolicySnapshot.ChargePoint.FINISH?RoomStatus.SETTLED:RoomStatus.ACTIVE;
        state.chargedAt=clock.instant();state.version++;
        return state.view();
    }

    public synchronized RoomBillingView chargeWinnerAtFinish(long roomId,CurrencyAccount winner){
        RoomState state=state(roomId);Objects.requireNonNull(winner,"winner");
        if(state.policy.paymentSubject()!=BillingPolicySnapshot.PaymentSubject.WINNER
                ||state.policy.chargePoint()!=BillingPolicySnapshot.ChargePoint.FINISH)
            throw new IllegalStateException("winner billing is not locked for this room");
        if(state.provisionStep!=ProvisionStep.TABLE_PUSHED)throw new IllegalStateException("room provisioning incomplete");
        if(!winner.currency().equals(state.policy.payerAccount().currency())||winner.scopeId()!=state.policy.payerAccount().scopeId())
            throw new IllegalArgumentException("winner asset scope differs from snapshot");
        Charge existing=state.charges.get(winner.playerId());
        if(existing==null){
            String businessId=RoomBillingBusinessIds.consume(roomId,winner.playerId());
            billing.debit(businessId,winner,state.policy.totalAmount(),"ROOM_WINNER_CHARGE");
            existing=new Charge(winner,state.policy.totalAmount(),businessId);existing.status=ChargeStatus.CONSUMED;
            state.charges.put(winner.playerId(),existing);
        }
        state.status=RoomStatus.SETTLED;state.chargedAt=clock.instant();state.version++;
        return state.view();
    }

    public synchronized RoomBillingView creationFailed(long roomId,ProvisionStep failedStep,String reason){
        RoomState state=state(roomId);Objects.requireNonNull(failedStep,"failedStep");requireReason(reason);
        if(state.terminal())return state.view();
        state.failure=failedStep+":"+reason;
        compensate(state,true);
        state.version++;
        return state.view();
    }

    public synchronized RoomBillingView cancel(long roomId,int roundsPlayed,String reason){
        RoomState state=state(roomId);requireReason(reason);
        if(roundsPlayed<0||roundsPlayed>state.policy.totalRounds())throw new IllegalArgumentException("invalid roundsPlayed");
        if(state.status==RoomStatus.CANCELLED||state.status==RoomStatus.COMPENSATED)return state.view();
        for(Charge charge:state.charges.values()){
            if(charge.status==ChargeStatus.RESERVED)release(state,charge,"ROOM_CANCEL."+reason);
            else if(charge.status==ChargeStatus.CONSUMED){
                long refund=state.policy.refundableAmount(charge.amount,roundsPlayed);
                if(refund>0)refund(state,charge,refund,roundsPlayed,"ROOM_CANCEL."+reason);
            }
        }
        state.roundsPlayed=roundsPlayed;
        state.status=state.charges.values().stream().anyMatch(c->c.status==ChargeStatus.COMPENSATION_PENDING)
                ?RoomStatus.COMPENSATION_PENDING:RoomStatus.CANCELLED;
        state.version++;
        return state.view();
    }

    public synchronized List<RoomBillingView> releaseExpired(){
        Instant now=clock.instant();List<RoomBillingView> changed=new ArrayList<>();
        for(RoomState state:rooms.values()){
            if(state.status!=RoomStatus.PROVISIONING||now.isBefore(state.expiresAt))continue;
            for(Charge charge:state.charges.values())if(charge.status==ChargeStatus.RESERVED)release(state,charge,"RESERVATION_EXPIRED");
            state.status=state.charges.values().stream().anyMatch(c->c.status==ChargeStatus.COMPENSATION_PENDING)
                    ?RoomStatus.COMPENSATION_PENDING:RoomStatus.COMPENSATED;
            state.failure="RESERVATION_EXPIRED";state.version++;changed.add(state.view());
        }
        return List.copyOf(changed);
    }

    public synchronized RoomBillingView retryCompensation(long roomId){
        RoomState state=state(roomId);
        if(state.status!=RoomStatus.COMPENSATION_PENDING)return state.view();
        compensate(state,true);state.version++;return state.view();
    }

    public synchronized Optional<RoomBillingView> find(long roomId){return Optional.ofNullable(rooms.get(roomId)).map(RoomState::view);}

    private void reserve(RoomState state,CurrencyAccount account,long amount){
        if(state.charges.containsKey(account.playerId()))throw new IllegalArgumentException("duplicate room payer");
        String businessId=RoomBillingBusinessIds.consume(state.roomId,account.playerId());
        billing.debit(businessId,account,positive(amount),"ROOM_RESERVE");
        state.charges.put(account.playerId(),new Charge(account,amount,businessId));
    }

    private void release(RoomState state,Charge charge,String reason){
        try{
            billing.credit(RoomBillingBusinessIds.release(state.roomId,charge.account.playerId()),charge.account,
                    charge.amount,"ROOM_RELEASE");
            charge.refunded=charge.amount;charge.status=ChargeStatus.RELEASED;
        }catch(RuntimeException failure){charge.status=ChargeStatus.COMPENSATION_PENDING;state.status=RoomStatus.COMPENSATION_PENDING;state.failure=reason;}
    }

    private void refund(RoomState state,Charge charge,long amount,int roundsPlayed,String reason){
        try{
            String businessId=roundsPlayed==0?RoomBillingBusinessIds.refund(state.roomId,charge.account.playerId())
                    :RoomBillingBusinessIds.partialRefund(state.roomId,charge.account.playerId(),roundsPlayed);
            billing.credit(businessId,charge.account,amount,"ROOM_REFUND");
            charge.refunded=amount;charge.status=amount==charge.amount?ChargeStatus.REFUNDED:ChargeStatus.PARTIALLY_REFUNDED;
        }catch(RuntimeException failure){charge.status=ChargeStatus.COMPENSATION_PENDING;state.status=RoomStatus.COMPENSATION_PENDING;state.failure=reason;}
    }

    private void compensate(RoomState state,boolean creationFailure){
        for(Charge charge:state.charges.values()){
            if(charge.status==ChargeStatus.RESERVED||charge.status==ChargeStatus.COMPENSATION_PENDING)release(state,charge,"CREATE_COMPENSATION");
            else if(creationFailure&&charge.status==ChargeStatus.CONSUMED)refund(state,charge,charge.amount,0,"CREATE_COMPENSATION");
        }
        state.status=state.charges.values().stream().anyMatch(c->c.status==ChargeStatus.COMPENSATION_PENDING)
                ?RoomStatus.COMPENSATION_PENDING:RoomStatus.COMPENSATED;
    }

    private static void validate(RoomChargeCommand command){
        if(command.roomId()<=0||command.charges()==null)throw new IllegalArgumentException("invalid room command");
        var subject=command.policy().paymentSubject();int size=command.charges().size();
        if(subject==BillingPolicySnapshot.PaymentSubject.AA){
            if(size>command.policy().maximumPlayers())throw new IllegalArgumentException("too many AA payers");
            for(long amount:command.charges().values())if(amount!=command.policy().amountForAaPlayer())throw new IllegalArgumentException("AA amount differs from locked snapshot");
        }else if(subject==BillingPolicySnapshot.PaymentSubject.WINNER){
            if(size!=0)throw new IllegalArgumentException("winner is selected only at finish");
        }else if(size!=1||command.charges().values().iterator().next()!=command.policy().totalAmount())
            throw new IllegalArgumentException("payer or amount differs from locked snapshot");
        for(CurrencyAccount account:command.charges().keySet())if(!account.currency().equals(command.policy().payerAccount().currency())
                ||account.scopeId()!=command.policy().payerAccount().scopeId())throw new IllegalArgumentException("asset scope differs from locked snapshot");
    }

    private RoomState state(long roomId){RoomState state=rooms.get(roomId);if(state==null)throw new IllegalArgumentException("room billing not found");return state;}
    private static Charge charge(RoomState state,long playerId){Charge charge=state.charges.get(playerId);if(charge==null)throw new IllegalArgumentException("payer not found");return charge;}
    private static long positive(long amount){if(amount<=0)throw new IllegalArgumentException("amount must be positive");return amount;}
    private static void requireReason(String reason){if(reason==null||!reason.matches("[A-Z0-9_.-]{1,32}"))throw new IllegalArgumentException("invalid reason");}

    public record RoomChargeCommand(long roomId,BillingPolicySnapshot policy,Map<CurrencyAccount,Long> charges){
        public RoomChargeCommand{Objects.requireNonNull(policy,"policy");charges=Map.copyOf(charges);}
    }
    public enum ProvisionStep { NONE, ROOM_REGISTERED, ROUTE_BOUND, TABLE_PUSHED }
    public enum RoomStatus { PROVISIONING, ACTIVE, SETTLED, CANCELLED, COMPENSATION_PENDING, COMPENSATED }
    public enum ChargeStatus { RESERVED, CONSUMED, RELEASED, PARTIALLY_REFUNDED, REFUNDED, COMPENSATION_PENDING }
    public record ChargeView(CurrencyAccount account,long amount,long refunded,ChargeStatus status,String reserveBusinessId){}
    public record RoomBillingView(long roomId,String policyFingerprint,BillingPolicySnapshot.DisplayView display,
            RoomStatus status,ProvisionStep provisionStep,Map<Long,ChargeView> charges,int roundsPlayed,
            Instant expiresAt,Instant chargedAt,long version,String failure){public RoomBillingView{charges=Map.copyOf(charges);}}

    private static final class Charge{
        private final CurrencyAccount account;private final long amount;private final String reserveBusinessId;
        private long refunded;private ChargeStatus status=ChargeStatus.RESERVED;
        private Charge(CurrencyAccount account,long amount,String reserveBusinessId){this.account=account;this.amount=amount;this.reserveBusinessId=reserveBusinessId;}
        private ChargeView view(){return new ChargeView(account,amount,refunded,status,reserveBusinessId);}
    }
    private static final class RoomState{
        private final long roomId;private final BillingPolicySnapshot policy;private final Map<CurrencyAccount,Long> requested;
        private final Map<Long,Charge> charges=new LinkedHashMap<>();private final Instant expiresAt;
        private RoomStatus status=RoomStatus.PROVISIONING;private ProvisionStep provisionStep=ProvisionStep.NONE;
        private int roundsPlayed;private Instant chargedAt;private long version=1;private String failure;
        private RoomState(RoomChargeCommand command,Instant now){roomId=command.roomId();policy=command.policy();requested=command.charges();expiresAt=now.plus(policy.reservationTtl());}
        private void requireSame(RoomChargeCommand command){if(roomId!=command.roomId()||!policy.fingerprint().equals(command.policy().fingerprint())||!requested.equals(command.charges()))throw new IllegalArgumentException("roomId reused with different billing command");}
        private boolean terminal(){return status==RoomStatus.CANCELLED||status==RoomStatus.COMPENSATED||status==RoomStatus.SETTLED;}
        private RoomBillingView view(){Map<Long,ChargeView> result=new LinkedHashMap<>();charges.forEach((id,value)->result.put(id,value.view()));return new RoomBillingView(roomId,policy.fingerprint(),policy.display(),status,provisionStep,result,roundsPlayed,expiresAt,chargedAt,version,failure);}
    }
}
