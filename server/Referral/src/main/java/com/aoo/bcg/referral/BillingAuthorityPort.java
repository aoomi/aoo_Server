package com.aoo.bcg.referral;

import java.math.BigDecimal;

@FunctionalInterface
public interface BillingAuthorityPort {
    void credit(String idempotencyKey,long playerId,long scopeId,String currency,BigDecimal amount,String reasonCode) throws Exception;
}
