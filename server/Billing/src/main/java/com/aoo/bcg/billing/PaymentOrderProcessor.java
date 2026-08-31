package com.aoo.bcg.billing;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Objects;

/** Signed callback, duplicate delivery, refund and manual-review state machine. */
public final class PaymentOrderProcessor {
    private static final int MAX_CAS_RETRIES=32;
    private final PaymentOrderRepository orders;private final BillingService billing;
    private final PaymentCallbackVerifier verifier;private final Clock clock;
    public PaymentOrderProcessor(PaymentOrderRepository orders,BillingService billing,PaymentCallbackVerifier verifier,Clock clock){
        this.orders=Objects.requireNonNull(orders);this.billing=Objects.requireNonNull(billing);this.verifier=Objects.requireNonNull(verifier);this.clock=Objects.requireNonNull(clock);
    }
    public PaymentOrder create(String orderId,CurrencyAccount buyer,PaymentProductSnapshot product){
        PaymentOrder candidate=PaymentOrder.create(orderId,buyer,product,clock.instant());
        if(orders.insert(candidate))return candidate;
        PaymentOrder existing=load(orderId);
        if(!existing.buyer().equals(buyer)||!existing.product().fingerprint().equals(product.fingerprint()))throw new IllegalArgumentException("orderId reused with different product");
        return existing;
    }
    public PaymentOrder callback(Callback callback){
        Objects.requireNonNull(callback,"callback");String canonical=callback.canonicalPayload();
        if(!verifier.verify(canonical,callback.signature()))throw new SecurityException("invalid payment callback signature");
        String digest=PaymentProductSnapshot.digest(canonical);
        for(int retry=0;retry<MAX_CAS_RETRIES;retry++){
            PaymentOrder current=load(callback.orderId());String prior=current.callbackDigests().get(callback.eventId());
            if(prior!=null){if(!prior.equals(digest))throw new SecurityException("callback event replayed with different payload");return current;}
            boolean matches=current.product().fingerprint().equals(callback.productFingerprint())
                    &&current.product().amountMinor()==callback.amountMinor()
                    &&current.product().fiatCurrency().equals(callback.fiatCurrency())
                    &&current.product().channelCode().equals(callback.channelCode())
                    &&!callback.paidAt().isBefore(current.createdAt())
                    &&!callback.paidAt().isAfter(clock.instant().plusSeconds(300));
            if(matches&&!orders.claimProviderTransaction(callback.providerTransactionId(),callback.orderId()))matches=false;
            var callbacks=new LinkedHashMap<>(current.callbackDigests());callbacks.put(callback.eventId(),digest);
            PaymentOrder.State nextState=matches?paidState(current.state()):PaymentOrder.State.REVIEW;
            Instant paidAt=matches&&current.paidAt()==null?callback.paidAt():current.paidAt();
            PaymentOrder next=new PaymentOrder(current.orderId(),current.buyer(),current.product(),nextState,
                    matches?callback.providerTransactionId():current.providerTransactionId(),callbacks,
                    matches?null:"CALLBACK_SNAPSHOT_MISMATCH",current.version()+1,current.createdAt(),paidAt,
                    current.deliveredAt(),current.refundedAt());
            if(orders.replace(current.version(),next))return next;
        }
        throw new IllegalStateException("payment callback contention");
    }
    public PaymentOrder deliver(String orderId){
        for(int retry=0;retry<MAX_CAS_RETRIES;retry++){
            PaymentOrder current=load(orderId);
            if(current.state()==PaymentOrder.State.DELIVERED||current.state()==PaymentOrder.State.REFUND_PENDING||current.state()==PaymentOrder.State.REFUNDED)return current;
            if(current.state()!=PaymentOrder.State.PAID)throw new IllegalStateException("order is not paid");
            billing.credit("payment:"+orderId+":deliver",current.buyer(),current.product().assetUnits(),"PAYMENT_DELIVER");
            PaymentOrder next=new PaymentOrder(current.orderId(),current.buyer(),current.product(),PaymentOrder.State.DELIVERED,
                    current.providerTransactionId(),current.callbackDigests(),null,current.version()+1,current.createdAt(),current.paidAt(),clock.instant(),null);
            if(orders.replace(current.version(),next))return next;
        }
        throw new IllegalStateException("payment delivery contention");
    }
    public PaymentOrder cancel(String orderId,long playerId,String reason){
        if(playerId<=0||reason==null||!reason.matches("CANCELLED_BY_PLAYER|PAYMENT_TIMEOUT"))throw new IllegalArgumentException("invalid payment cancellation");
        for(int retry=0;retry<MAX_CAS_RETRIES;retry++){
            PaymentOrder current=load(orderId);
            if(current.buyer().playerId()!=playerId)throw new SecurityException("payment order owner mismatch");
            if(current.state()==PaymentOrder.State.CLOSED)return current;
            if(current.state()!=PaymentOrder.State.CREATED)throw new IllegalStateException("paid payment order cannot be cancelled");
            PaymentOrder closed=new PaymentOrder(current.orderId(),current.buyer(),current.product(),PaymentOrder.State.CLOSED,
                    null,current.callbackDigests(),reason,current.version()+1,current.createdAt(),null,null,null);
            if(orders.replace(current.version(),closed))return closed;
        }
        throw new IllegalStateException("payment cancellation contention");
    }
    public PaymentOrder refund(String orderId,String refundRequestId){
        if(refundRequestId==null||!refundRequestId.matches("[A-Za-z0-9_.:-]{1,64}"))throw new IllegalArgumentException("invalid refund request");
        for(int retry=0;retry<MAX_CAS_RETRIES;retry++){
            PaymentOrder current=load(orderId);
            if(current.state()==PaymentOrder.State.REFUNDED)return current;
            if(current.state()!=PaymentOrder.State.DELIVERED&&current.state()!=PaymentOrder.State.REFUND_PENDING)throw new IllegalStateException("order cannot be refunded");
            PaymentOrder pending=current.state()==PaymentOrder.State.REFUND_PENDING?current:new PaymentOrder(current.orderId(),current.buyer(),current.product(),PaymentOrder.State.REFUND_PENDING,
                    current.providerTransactionId(),current.callbackDigests(),null,current.version()+1,current.createdAt(),current.paidAt(),current.deliveredAt(),null);
            if(current.state()!=PaymentOrder.State.REFUND_PENDING&&!orders.replace(current.version(),pending))continue;
            billing.debit("payment:"+orderId+":refund",pending.buyer(),pending.product().assetUnits(),"PAYMENT_REFUND");
            PaymentOrder refunded=new PaymentOrder(pending.orderId(),pending.buyer(),pending.product(),PaymentOrder.State.REFUNDED,
                    pending.providerTransactionId(),pending.callbackDigests(),null,pending.version()+1,pending.createdAt(),pending.paidAt(),pending.deliveredAt(),clock.instant());
            if(orders.replace(pending.version(),refunded))return refunded;
        }
        throw new IllegalStateException("payment refund contention");
    }
    private PaymentOrder load(String orderId){return orders.find(orderId).orElseThrow(()->new IllegalArgumentException("payment order not found"));}
    private static PaymentOrder.State paidState(PaymentOrder.State current){return switch(current){case CREATED->PaymentOrder.State.PAID;case PAID,DELIVERED,REFUND_PENDING,REFUNDED->current;case CLOSED,REVIEW->PaymentOrder.State.REVIEW;};}

    public record Callback(String eventId,String orderId,String providerTransactionId,String channelCode,
            long amountMinor,String fiatCurrency,String productFingerprint,Instant paidAt,String signature){
        public Callback{
            if(eventId==null||!eventId.matches("[A-Za-z0-9_.:-]{1,128}")||orderId==null||orderId.isBlank()
                    ||providerTransactionId==null||!providerTransactionId.matches("[A-Za-z0-9_.:-]{1,128}")
                    ||channelCode==null||!channelCode.matches("[A-Z0-9_]{1,32}")||amountMinor<=0
                    ||fiatCurrency==null||!fiatCurrency.matches("[A-Z]{3}")||productFingerprint==null||paidAt==null)
                throw new IllegalArgumentException("invalid payment callback");
        }
        public String canonicalPayload(){return String.join("|",eventId,orderId,providerTransactionId,channelCode,
                Long.toString(amountMinor),fiatCurrency,productFingerprint,paidAt.toString());}
        public Callback signed(PaymentCallbackVerifier verifier){return new Callback(eventId,orderId,providerTransactionId,channelCode,amountMinor,fiatCurrency,productFingerprint,paidAt,verifier.sign(canonicalPayload()));}
    }
}
