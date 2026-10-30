package com.aoo.bcg.common.settlement;

import java.util.Objects;

/** Fixed validation and persistence pipeline shared by every game. */
public final class SettlementExecutor {
    private final SettlementRepository repository;

    public SettlementExecutor(SettlementRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public SettlementResult execute(SettlementScope scope, SettlementBalancePolicy balancePolicy,
            long roomId, int roundNo, String playVersion, SettlementResult result) {
        Objects.requireNonNull(scope, "scope");
        Objects.requireNonNull(balancePolicy, "balancePolicy");
        SettlementValidator.validate(result, roomId, roundNo, playVersion, balancePolicy);
        String businessId = switch (scope) {
            case ROUND -> SettlementBusinessId.round(roomId, roundNo, playVersion);
            case FINAL_ROOM -> SettlementBusinessId.finalRoom(roomId, playVersion);
        };
        return repository.save(businessId, result);
    }
}
