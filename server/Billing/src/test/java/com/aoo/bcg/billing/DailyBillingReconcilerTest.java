package com.aoo.bcg.billing;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DailyBillingReconcilerTest {
    @Test void reconcilesChannelLedgerBalancesRoomResultAndDisplayFromOneSnapshot(){
        var a=new CurrencyAccount(1,"ROOM_CARD");var buyer=new CurrencyAccount(2,"ROOM_CARD");Instant start=Instant.parse("2026-08-24T00:00:00Z");Instant end=start.plusSeconds(86400);
        var ledger=List.of(new DailyBillingReconciler.LedgerObservation("reserve",a,-30,100,70,start.plusSeconds(1)),new DailyBillingReconciler.LedgerObservation("refund",a,15,70,85,start.plusSeconds(2)),new DailyBillingReconciler.LedgerObservation("payment:o:deliver",buyer,100,0,100,start.plusSeconds(3)));
        var balances=List.of(new DailyBillingReconciler.BalanceSnapshot(a,100,85),new DailyBillingReconciler.BalanceSnapshot(buyer,0,100));
        var payments=List.of(new DailyBillingReconciler.PaymentObservation("o","WECHAT",1000,true,true,false,"payment:o:deliver","payment:o:refund"));
        var rooms=List.of(new DailyBillingReconciler.RoomObservation("room:1",RoomChargeCoordinator.ChargeStatus.PARTIALLY_REFUNDED,"reserve","refund",15,true,"fingerprint","fingerprint",end.plusSeconds(1)));
        var input=new DailyBillingReconciler.Input(LocalDate.of(2026,8,24),end,ledger,balances,payments,Map.of("WECHAT",1000L),rooms);
        assertTrue(new DailyBillingReconciler().reconcile(input).balanced());
    }
    @Test void reportsAllCrossDomainDifferencesForAutomaticWorkOrders(){
        var account=new CurrencyAccount(1,"ROOM_CARD");Instant end=Instant.parse("2026-08-25T00:00:00Z");
        var input=new DailyBillingReconciler.Input(LocalDate.of(2026,8,24),end,List.of(),List.of(new DailyBillingReconciler.BalanceSnapshot(account,10,9)),List.of(new DailyBillingReconciler.PaymentObservation("o","WECHAT",100,true,true,true,"deliver","refund")),Map.of("WECHAT",99L),List.of(new DailyBillingReconciler.RoomObservation("room",RoomChargeCoordinator.ChargeStatus.RESERVED,"reserve","refund",0,true,"locked","client",end)));
        var types=new DailyBillingReconciler().reconcile(input).differences().stream().map(DailyBillingReconciler.Difference::type).collect(java.util.stream.Collectors.toSet());
        assertTrue(types.containsAll(java.util.Set.of("CLOSING_BALANCE","PAID_NOT_DELIVERED","REFUND_LEDGER_MISSING","CHANNEL_TOTAL","ROOM_CHARGE_LEDGER_MISSING","BUSINESS_RESULT_UNCHARGED","DISPLAY_POLICY_DRIFT","STALE_RESERVATION")));
    }
}
