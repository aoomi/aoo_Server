package com.aoo.bcg.common.settlement;

import java.util.Optional;

public interface SettlementRepository {
    SettlementResult save(String businessId, SettlementResult result);
    Optional<SettlementResult> find(String businessId);
}
