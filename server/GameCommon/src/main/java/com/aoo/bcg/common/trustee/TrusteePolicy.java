package com.aoo.bcg.common.trustee;

import java.util.List;

public interface TrusteePolicy<S> {
    PlayerIntent choose(long playerId, S playerVisibleState, List<PlayerIntent> legalIntents);
}
