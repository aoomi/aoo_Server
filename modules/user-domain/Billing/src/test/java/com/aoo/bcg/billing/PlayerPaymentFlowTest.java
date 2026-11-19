package com.aoo.bcg.billing;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlayerPaymentFlowTest {
    private static final Clock CLOCK=Clock.fixed(Instant.parse("2026-08-24T12:00:00Z"),ZoneOffset.UTC);
    @Test void checkoutIsHttpsSignedAndContainsLockedServerSnapshot(){
        var order=PaymentOrder.create("player-order-1",new CurrencyAccount(7,"ROOM_CARD"),product(),CLOCK.instant());
        var adapter=new HostedPaymentProviderAdapter(URI.create("https://pay.example.test/checkout"),"aoo-merchant","01234567890123456789012345678901",CLOCK);
        var checkout=adapter.checkout(order);
        assertTrue(checkout.checkoutUrl().startsWith("https://pay.example.test/checkout?"));assertTrue(checkout.checkoutUrl().contains("amountMinor=1000"));assertTrue(checkout.checkoutUrl().contains("productFingerprint="));assertTrue(checkout.checkoutUrl().contains("signature="));assertEquals(CLOCK.instant().plusSeconds(900),checkout.expiresAt());
        assertThrows(IllegalArgumentException.class,()->new HostedPaymentProviderAdapter(URI.create("http://pay.example.test"),"merchant","01234567890123456789012345678901",CLOCK));
    }
    @Test void ownerCanCancelCreatedOrderButCannotCancelPaidOrAnotherPlayersOrder(){
        var repo=new Orders();var billing=new InMemoryBillingService(new InMemoryLedgerRepository(),CLOCK,Map.of(),"ROOM_CARD");var verifier=new PaymentCallbackVerifier("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8));var processor=new PaymentOrderProcessor(repo,billing,verifier,CLOCK);
        processor.create("o-cancel",new CurrencyAccount(7,"ROOM_CARD"),product());assertEquals("CANCELLED_BY_PLAYER",processor.cancel("o-cancel",7,"CANCELLED_BY_PLAYER").failureCode());assertEquals(PaymentOrder.State.CLOSED,processor.cancel("o-cancel",7,"CANCELLED_BY_PLAYER").state());
        processor.create("o-owner",new CurrencyAccount(7,"ROOM_CARD"),product());assertThrows(SecurityException.class,()->processor.cancel("o-owner",8,"CANCELLED_BY_PLAYER"));
        processor.create("o-paid",new CurrencyAccount(7,"ROOM_CARD"),product());var callback=new PaymentOrderProcessor.Callback("event-1","o-paid","txn-1","WECHAT",1000,"CNY",product().fingerprint(),CLOCK.instant(),null).signed(verifier);processor.callback(callback);assertThrows(IllegalStateException.class,()->processor.cancel("o-paid",7,"CANCELLED_BY_PLAYER"));
    }
    private static PaymentProductSnapshot product(){return PaymentProductSnapshot.lock("ROOM_CARD_100",3,"ROOM_CARD",100,"CNY",1000,"WECHAT",CLOCK.instant());}
    private static final class Orders implements PaymentOrderRepository{private final Map<String,PaymentOrder> values=new HashMap<>();private final Map<String,String> transactions=new HashMap<>();public Optional<PaymentOrder> find(String id){return Optional.ofNullable(values.get(id));}public boolean insert(PaymentOrder order){return values.putIfAbsent(order.orderId(),order)==null;}public boolean replace(long expected,PaymentOrder order){PaymentOrder current=values.get(order.orderId());if(current==null||current.version()!=expected)return false;values.put(order.orderId(),order);return true;}public boolean claimProviderTransaction(String transaction,String order){String prior=transactions.putIfAbsent(transaction,order);return prior==null||prior.equals(order);}}
}
