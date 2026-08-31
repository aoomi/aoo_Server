package com.aoo.bcg.common.trustee;

import java.util.List;

public final class FirstLegalIntentTrusteePolicy<S> implements TrusteePolicy<S> {
    @Override public PlayerIntent choose(long playerId, S playerVisibleState, List<PlayerIntent> legalIntents) {
        if (legalIntents.isEmpty()) throw new IllegalStateException("no legal trustee intent");
        return legalIntents.getFirst();
    }
}
