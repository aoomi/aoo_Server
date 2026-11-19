package com.aoo.bcg.common.settlement;
public interface SettlementPolicy<S> { SettlementResult settle(S authoritativeState); }
