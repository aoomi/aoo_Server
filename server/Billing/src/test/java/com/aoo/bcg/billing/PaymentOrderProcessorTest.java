package com.aoo.bcg.billing;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PaymentOrderProcessorTest {
    @Test void signedCallbackIsSnapshotCheckedDeliveredOnceAndRefundedOnce(){
        var repo=new Orders();var ledger=new InMemoryLedgerRepository();var billing=new InMemoryBillingService(ledger,Clock.systemUTC(),Map.of(),"ROOM_CARD");var buyer=new CurrencyAccount(7,"ROOM_CARD");
        var verifier=verifier();var processor=new PaymentOrderProcessor(repo,billing,verifier,Clock.fixed(Instant.EPOCH,ZoneOffset.UTC));var product=product();
        processor.create("o-1",buyer,product);var callback=callback("event-1","o-1","txn-1",product,1000).signed(verifier);
        assertEquals(PaymentOrder.State.PAID,processor.callback(callback).state());assertEquals(PaymentOrder.State.PAID,processor.callback(callback).state());
        assertEquals(PaymentOrder.State.DELIVERED,processor.deliver("o-1").state());assertEquals(100,billing.balance(buyer));
        processor.deliver("o-1");assertEquals(100,billing.balance(buyer));
        assertEquals(PaymentOrder.State.REFUNDED,processor.refund("o-1","request-1").state());assertEquals(0,billing.balance(buyer));
        processor.refund("o-1","request-2");assertEquals(0,billing.balance(buyer));
        assertTrue(ledger.findByBusinessId("payment:o-1:deliver").isPresent());assertTrue(ledger.findByBusinessId("payment:o-1:refund").isPresent());
    }
    @Test void tamperSnapshotReplayAndProviderTransactionReuseAreRejectedOrReviewed(){
        var repo=new Orders();var billing=new InMemoryBillingService(new InMemoryLedgerRepository(),Clock.systemUTC(),Map.of(),"ROOM_CARD");var verifier=verifier();var processor=new PaymentOrderProcessor(repo,billing,verifier,Clock.fixed(Instant.EPOCH,ZoneOffset.UTC));var product=product();
        processor.create("o-1",new CurrencyAccount(7,"ROOM_CARD"),product);processor.create("o-2",new CurrencyAccount(8,"ROOM_CARD"),product);
        var valid=callback("event-1","o-1","txn-1",product,1000).signed(verifier);assertEquals(PaymentOrder.State.PAID,processor.callback(valid).state());
        var unsigned=callback("event-x","o-2","txn-x",product,1000);assertThrows(SecurityException.class,()->processor.callback(unsigned));
        var collision=callback("event-2","o-2","txn-1",product,1000).signed(verifier);assertEquals(PaymentOrder.State.REVIEW,processor.callback(collision).state());
        processor.create("o-3",new CurrencyAccount(9,"ROOM_CARD"),product);var wrongAmount=callback("event-3","o-3","txn-3",product,999).signed(verifier);assertEquals(PaymentOrder.State.REVIEW,processor.callback(wrongAmount).state());
        var replayChanged=callback("event-1","o-1","txn-1",product,999).signed(verifier);assertThrows(SecurityException.class,()->processor.callback(replayChanged));
        assertThrows(IllegalStateException.class,()->processor.deliver("o-3"));
    }
    private static PaymentCallbackVerifier verifier(){return new PaymentCallbackVerifier("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));}
    private static PaymentProductSnapshot product(){return PaymentProductSnapshot.lock("ROOM_CARD_100",3,"ROOM_CARD",100,"CNY",1000,"WECHAT",Instant.EPOCH);}
    private static PaymentOrderProcessor.Callback callback(String event,String order,String txn,PaymentProductSnapshot product,long amount){return new PaymentOrderProcessor.Callback(event,order,txn,"WECHAT",amount,"CNY",product.fingerprint(),Instant.EPOCH.plusSeconds(1),null);}
    private static final class Orders implements PaymentOrderRepository{
        private final Map<String,PaymentOrder> values=new HashMap<>();private final Map<String,String> transactions=new HashMap<>();
        @Override public synchronized Optional<PaymentOrder> find(String id){return Optional.ofNullable(values.get(id));}
        @Override public synchronized boolean insert(PaymentOrder order){return values.putIfAbsent(order.orderId(),order)==null;}
        @Override public synchronized boolean replace(long expected,PaymentOrder order){PaymentOrder current=values.get(order.orderId());if(current==null||current.version()!=expected)return false;values.put(order.orderId(),order);return true;}
        @Override public synchronized boolean claimProviderTransaction(String transaction,String order){String current=transactions.putIfAbsent(transaction,order);return current==null||current.equals(order);}
    }
}
