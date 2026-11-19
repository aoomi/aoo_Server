package com.aoo.bcg.billing;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Cross-checks channel orders, immutable ledger, balances, reservations and room business results. */
public final class DailyBillingReconciler {
    public Report reconcile(Input input){
        Objects.requireNonNull(input,"input");List<Difference> differences=new ArrayList<>();
        Map<String,LedgerObservation> byBusiness=new HashMap<>();
        for(LedgerObservation entry:input.ledger())if(byBusiness.putIfAbsent(entry.businessId(),entry)!=null)
            differences.add(diff("DUPLICATE_BUSINESS_ID",entry.businessId(),"unique","duplicate"));
        reconcileBalances(input,byBusiness,differences);reconcileOrders(input,byBusiness,differences);reconcileRooms(input,byBusiness,differences);
        differences.sort(Comparator.comparing(Difference::type).thenComparing(Difference::businessId));
        return new Report(input.businessDate(),differences);
    }
    private static void reconcileBalances(Input input,Map<String,LedgerObservation> byBusiness,List<Difference> out){
        Map<CurrencyAccount,List<LedgerObservation>> grouped=new HashMap<>();
        input.ledger().forEach(entry->grouped.computeIfAbsent(entry.account(),ignored->new ArrayList<>()).add(entry));
        Set<CurrencyAccount> seen=new HashSet<>();
        for(BalanceSnapshot balance:input.balances()){
            if(!seen.add(balance.account()))out.add(diff("DUPLICATE_BALANCE",balance.account().canonicalId(),"one snapshot","duplicate"));
            List<LedgerObservation> entries=grouped.getOrDefault(balance.account(),List.of()).stream().sorted(Comparator.comparing(LedgerObservation::createdAt).thenComparing(LedgerObservation::businessId)).toList();
            long running=balance.openingBalance();
            for(LedgerObservation entry:entries){
                if(entry.balanceBefore()!=running)out.add(diff("BALANCE_CHAIN",entry.businessId(),Long.toString(running),Long.toString(entry.balanceBefore())));
                running=Math.addExact(running,entry.delta());
                if(entry.balanceAfter()!=running)out.add(diff("ENTRY_AFTER_BALANCE",entry.businessId(),Long.toString(running),Long.toString(entry.balanceAfter())));
            }
            if(running!=balance.closingBalance())out.add(diff("CLOSING_BALANCE",balance.account().canonicalId(),Long.toString(running),Long.toString(balance.closingBalance())));
        }
        for(CurrencyAccount account:grouped.keySet())if(!seen.contains(account))out.add(diff("MISSING_BALANCE_SNAPSHOT",account.canonicalId(),"opening and closing","missing"));
    }
    private static void reconcileOrders(Input input,Map<String,LedgerObservation> ledger,List<Difference> out){
        Map<String,Long> totals=new HashMap<>();
        for(PaymentObservation order:input.payments()){
            if(order.paid())totals.merge(order.channelCode(),order.amountMinor(),Math::addExact);
            if(order.paid()&&!order.delivered())out.add(diff("PAID_UNDELIVERED",order.orderId(),"delivered","pending"));
            if(order.delivered()&&!ledger.containsKey(order.deliveryBusinessId()))out.add(diff("PAID_NOT_DELIVERED",order.orderId(),order.deliveryBusinessId(),"missing ledger"));
            if(!order.delivered()&&ledger.containsKey(order.deliveryBusinessId()))out.add(diff("DELIVERY_STATE_MISMATCH",order.orderId(),"not delivered","delivery ledger exists"));
            if(order.refunded()&&!ledger.containsKey(order.refundBusinessId()))out.add(diff("REFUND_LEDGER_MISSING",order.orderId(),order.refundBusinessId(),"missing ledger"));
        }
        Set<String> channels=new HashSet<>(totals.keySet());channels.addAll(input.channelPaidTotals().keySet());
        for(String channel:channels){long expected=totals.getOrDefault(channel,0L),actual=input.channelPaidTotals().getOrDefault(channel,0L);if(expected!=actual)out.add(diff("CHANNEL_TOTAL",channel,Long.toString(expected),Long.toString(actual)));}
    }
    private static void reconcileRooms(Input input,Map<String,LedgerObservation> ledger,List<Difference> out){
        for(RoomObservation room:input.rooms()){
            if((room.chargeStatus()==RoomChargeCoordinator.ChargeStatus.RESERVED||room.chargeStatus()==RoomChargeCoordinator.ChargeStatus.CONSUMED)
                    &&!ledger.containsKey(room.reserveBusinessId()))out.add(diff("ROOM_CHARGE_LEDGER_MISSING",room.businessId(),room.reserveBusinessId(),"missing"));
            if(room.refundedAmount()>0&&!ledger.containsKey(room.refundBusinessId()))out.add(diff("ROOM_REFUND_LEDGER_MISSING",room.businessId(),room.refundBusinessId(),"missing"));
            if(room.businessResultExists()&&room.chargeStatus()==RoomChargeCoordinator.ChargeStatus.RESERVED)
                out.add(diff("BUSINESS_RESULT_UNCHARGED",room.businessId(),"consumed","reserved"));
            if(!room.policyFingerprint().equals(room.displayFingerprint()))out.add(diff("DISPLAY_POLICY_DRIFT",room.businessId(),room.policyFingerprint(),room.displayFingerprint()));
            if(room.chargeStatus()==RoomChargeCoordinator.ChargeStatus.RESERVED&&!room.expiresAt().isAfter(input.windowEnd()))
                out.add(diff("STALE_RESERVATION",room.businessId(),"released by "+input.windowEnd(),room.expiresAt().toString()));
        }
    }
    private static Difference diff(String type,String id,String expected,String actual){return new Difference(type,id,expected,actual);}
    public record Input(LocalDate businessDate,Instant windowEnd,List<LedgerObservation> ledger,List<BalanceSnapshot> balances,
            List<PaymentObservation> payments,Map<String,Long> channelPaidTotals,List<RoomObservation> rooms){
        public Input{Objects.requireNonNull(businessDate);Objects.requireNonNull(windowEnd);ledger=List.copyOf(ledger);balances=List.copyOf(balances);payments=List.copyOf(payments);channelPaidTotals=Map.copyOf(channelPaidTotals);rooms=List.copyOf(rooms);}
    }
    public record LedgerObservation(String businessId,CurrencyAccount account,long delta,long balanceBefore,long balanceAfter,Instant createdAt){
        public LedgerObservation{if(businessId==null||businessId.isBlank()||account==null||delta==0||Math.addExact(balanceBefore,delta)!=balanceAfter||createdAt==null)throw new IllegalArgumentException("invalid ledger observation");}
        public static LedgerObservation global(LedgerEntry entry){return new LedgerObservation(entry.businessId(),new CurrencyAccount(entry.playerId(),entry.currency()),entry.delta(),entry.balanceBefore(),entry.balanceAfter(),entry.createdAt());}
    }
    public record BalanceSnapshot(CurrencyAccount account,long openingBalance,long closingBalance){public BalanceSnapshot{if(account==null||openingBalance<0||closingBalance<0)throw new IllegalArgumentException("invalid balance snapshot");}}
    public record PaymentObservation(String orderId,String channelCode,long amountMinor,boolean paid,boolean delivered,boolean refunded,String deliveryBusinessId,String refundBusinessId){public PaymentObservation{if(orderId==null||channelCode==null||amountMinor<=0||deliveryBusinessId==null||refundBusinessId==null)throw new IllegalArgumentException("invalid payment observation");}}
    public record RoomObservation(String businessId,RoomChargeCoordinator.ChargeStatus chargeStatus,String reserveBusinessId,String refundBusinessId,long refundedAmount,boolean businessResultExists,String policyFingerprint,String displayFingerprint,Instant expiresAt){public RoomObservation{if(businessId==null||chargeStatus==null||reserveBusinessId==null||refundBusinessId==null||refundedAmount<0||policyFingerprint==null||displayFingerprint==null||expiresAt==null)throw new IllegalArgumentException("invalid room observation");}}
    public record Difference(String type,String businessId,String expected,String actual){}
    public record Report(LocalDate businessDate,List<Difference> differences){public Report{differences=List.copyOf(differences);}public boolean balanced(){return differences.isEmpty();}}
}
