package com.aoo.bcg.common.settlement;

/** Whether player score deltas must balance inside this settlement. */
public enum SettlementBalancePolicy {
    ZERO_SUM,
    NON_ZERO_SUM;

    boolean requiresZeroSum() { return this == ZERO_SUM; }
}
