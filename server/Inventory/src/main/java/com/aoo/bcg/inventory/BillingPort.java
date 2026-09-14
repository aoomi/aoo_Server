package com.aoo.bcg.inventory;

/** The only money boundary used by Store; implementations must call Billing rather than own balances. */
public interface BillingPort {
    void debit(String idempotencyKey,long playerId,String currency,long amount,String reason);
    void refund(String idempotencyKey,long playerId,String currency,long amount,String reason);
}
