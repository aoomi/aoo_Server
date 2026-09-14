package com.aoo.bcg.billing;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Authoritative GOLD buy-in escrow, zero-sum result, fee and abnormal-exit closure. */
public final class GoldTableSettlementService {
    private final BillingService billing;private final Clock clock;private final Map<Long,Table> tables=new LinkedHashMap<>();
    public GoldTableSettlementService(BillingService billing,Clock clock){this.billing=Objects.requireNonNull(billing);this.clock=Objects.requireNonNull(clock);}

    public synchronized TableView enter(long tableId,CurrencyAccount account,long buyIn){
        if(tableId<=0||buyIn<=0||!account.currency().equals("GOLD")||account.scopeId()!=0)throw new IllegalArgumentException("invalid gold buy-in");
        Table table=tables.computeIfAbsent(tableId,id->new Table(id,clock.instant()));
        if(table.status!=Status.OPEN)throw new IllegalStateException("gold table is not open");
        Seat existing=table.seats.get(account.playerId());
        if(existing!=null){if(!existing.account.equals(account)||existing.buyIn!=buyIn)throw new IllegalArgumentException("player buy-in changed");return table.view();}
        billing.debit(id(tableId,account.playerId(),"buyin"),account,buyIn,"GOLD_BUYIN");
        table.seats.put(account.playerId(),new Seat(account,buyIn));table.version++;return table.view();
    }

    public synchronized TableView settle(long tableId,Map<Long,Long> netResults,int serviceFeeBasisPoints,String settlementVersion){
        Table table=table(tableId);
        if(serviceFeeBasisPoints<0||serviceFeeBasisPoints>10_000||settlementVersion==null||!settlementVersion.matches("[A-Za-z0-9_.-]{1,32}"))throw new IllegalArgumentException("invalid gold settlement");
        if(table.status==Status.SETTLED){if(!table.settlementVersion.equals(settlementVersion)||!table.netResults.equals(netResults))throw new IllegalArgumentException("settlement version reused");return table.view();}
        if(table.status==Status.SETTLEMENT_PENDING&&(!table.settlementVersion.equals(settlementVersion)||!table.netResults.equals(netResults)))throw new IllegalArgumentException("pending settlement changed");
        if(table.status==Status.ABORTED)throw new IllegalStateException("gold table aborted");
        if(!netResults.keySet().equals(table.seats.keySet()))throw new IllegalArgumentException("gold result players differ from buy-ins");
        long netSum=0;for(long result:netResults.values())netSum=Math.addExact(netSum,result);
        if(netSum!=0)throw new IllegalArgumentException("gold results must be zero sum before fee");
        table.status=Status.SETTLEMENT_PENDING;table.settlementVersion=settlementVersion;table.netResults=Map.copyOf(netResults);
        long fees=0;
        for(Seat seat:table.seats.values()){
            long net=netResults.get(seat.account.playerId());long gross=Math.addExact(seat.buyIn,net);
            if(gross<0)throw new IllegalArgumentException("loss exceeds frozen buy-in");
            long fee=net>0?Math.floorDiv(Math.multiplyExact(net,serviceFeeBasisPoints),10_000):0;
            long payout=Math.subtractExact(gross,fee);fees=Math.addExact(fees,fee);
            if(payout>0)billing.credit(id(tableId,seat.account.playerId(),"settle-"+settlementVersion),seat.account,payout,"GOLD_SETTLE");
            seat.netResult=net;seat.serviceFee=fee;seat.payout=payout;
        }
        table.totalServiceFee=fees;table.status=Status.SETTLED;table.closedAt=clock.instant();table.version++;return table.view();
    }

    /** Used only when no authoritative result exists; every frozen buy-in is released idempotently. */
    public synchronized TableView abort(long tableId,String reason){
        if(reason==null||!reason.matches("[A-Z0-9_.-]{1,32}"))throw new IllegalArgumentException("invalid abort reason");
        Table table=table(tableId);if(table.status==Status.ABORTED)return table.view();if(table.status!=Status.OPEN)throw new IllegalStateException("settlement already started");
        table.status=Status.ABORT_PENDING;
        for(Seat seat:table.seats.values()){
            billing.credit(id(tableId,seat.account.playerId(),"abort"),seat.account,seat.buyIn,"GOLD_ABORT");seat.payout=seat.buyIn;
        }
        table.status=Status.ABORTED;table.closedAt=clock.instant();table.failure=reason;table.version++;return table.view();
    }
    public synchronized Optional<TableView> find(long tableId){return Optional.ofNullable(tables.get(tableId)).map(Table::view);}
    private Table table(long tableId){Table table=tables.get(tableId);if(table==null)throw new IllegalArgumentException("gold table not found");return table;}
    private static String id(long tableId,long playerId,String action){return "gold:"+tableId+":player:"+playerId+":"+action;}
    public enum Status{OPEN,SETTLEMENT_PENDING,SETTLED,ABORT_PENDING,ABORTED}
    public record SeatView(CurrencyAccount account,long buyIn,long netResult,long serviceFee,long payout){}
    public record TableView(long tableId,Status status,Map<Long,SeatView> seats,long totalServiceFee,String settlementVersion,
            Map<Long,Long> netResults,Instant openedAt,Instant closedAt,long version,String failure){public TableView{seats=Map.copyOf(seats);netResults=Map.copyOf(netResults);}}
    private static final class Seat{private final CurrencyAccount account;private final long buyIn;private long netResult,serviceFee,payout;private Seat(CurrencyAccount account,long buyIn){this.account=account;this.buyIn=buyIn;}private SeatView view(){return new SeatView(account,buyIn,netResult,serviceFee,payout);}}
    private static final class Table{private final long tableId;private final Instant openedAt;private final Map<Long,Seat> seats=new LinkedHashMap<>();private Status status=Status.OPEN;private long totalServiceFee,version=1;private String settlementVersion,failure;private Map<Long,Long> netResults=Map.of();private Instant closedAt;private Table(long tableId,Instant openedAt){this.tableId=tableId;this.openedAt=openedAt;}private TableView view(){Map<Long,SeatView> result=new LinkedHashMap<>();seats.forEach((id,seat)->result.put(id,seat.view()));return new TableView(tableId,status,result,totalServiceFee,settlementVersion,netResults,openedAt,closedAt,version,failure);}}
}
