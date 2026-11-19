package com.aoo.bcg.billing;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/** Persistable payment aggregate. Every transition increments version for repository CAS. */
public record PaymentOrder(String orderId,CurrencyAccount buyer,PaymentProductSnapshot product,State state,
        String providerTransactionId,Map<String,String> callbackDigests,String failureCode,long version,
        Instant createdAt,Instant paidAt,Instant deliveredAt,Instant refundedAt){
    public PaymentOrder{
        if(orderId==null||!orderId.matches("[A-Za-z0-9_.:-]{1,128}")||version<=0)throw new IllegalArgumentException("invalid payment order");
        Objects.requireNonNull(buyer,"buyer");Objects.requireNonNull(product,"product");Objects.requireNonNull(state,"state");Objects.requireNonNull(createdAt,"createdAt");
        callbackDigests=Map.copyOf(callbackDigests);
        if(!buyer.currency().equals(product.assetCurrency()))throw new IllegalArgumentException("product asset differs from buyer account");
        if((state==State.PAID||state==State.DELIVERED||state==State.REFUND_PENDING||state==State.REFUNDED)&&paidAt==null)
            throw new IllegalArgumentException("paid order lacks paidAt");
        if((state==State.DELIVERED||state==State.REFUND_PENDING||state==State.REFUNDED)&&deliveredAt==null)
            throw new IllegalArgumentException("delivered order lacks deliveredAt");
        if(state==State.REFUNDED&&refundedAt==null)throw new IllegalArgumentException("refunded order lacks refundedAt");
    }
    public static PaymentOrder create(String orderId,CurrencyAccount buyer,PaymentProductSnapshot product,Instant now){
        return new PaymentOrder(orderId,buyer,product,State.CREATED,null,Map.of(),null,1,now,null,null,null);
    }
    public enum State { CREATED, PAID, DELIVERED, REFUND_PENDING, REFUNDED, CLOSED, REVIEW }
}
