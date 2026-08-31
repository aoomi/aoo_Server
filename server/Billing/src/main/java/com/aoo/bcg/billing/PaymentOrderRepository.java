package com.aoo.bcg.billing;

import java.util.Optional;

/** Implementations must enforce unique order id, provider transaction id and compare-and-set version. */
public interface PaymentOrderRepository {
    Optional<PaymentOrder> find(String orderId);
    boolean insert(PaymentOrder order);
    boolean replace(long expectedVersion,PaymentOrder order);
    boolean claimProviderTransaction(String providerTransactionId,String orderId);
}
