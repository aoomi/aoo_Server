package com.aoo.bcg.billing;

import java.time.LocalDate;
import java.util.Optional;

/** Storage must atomically enforce business id and the daily source/campaign limit. */
public interface AssetGrantRepository {
    Optional<AssetGrant> find(String businessId);
    boolean insert(AssetGrant grant);
    boolean replace(long expectedVersion,AssetGrant grant);
    boolean reserveDailyLimit(String limitKey,LocalDate date,String businessId,long amount,long maximum);
}
