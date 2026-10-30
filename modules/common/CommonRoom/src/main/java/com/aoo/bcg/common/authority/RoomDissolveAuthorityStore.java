package com.aoo.bcg.common.authority;

import com.aoo.bcg.gamespi.RoomOrchestrator;

/**
 * Durable dissolve authority port.  Implementations must use the canonical
 * runtime identity and room-lock metadata sources; process-local vote state is
 * not an implementation of this contract.
 */
public interface RoomDissolveAuthorityStore {
    RoomOrchestrator.DissolveResult apply(RoomOrchestrator.DissolveApplyCommand command);

    RoomOrchestrator.DissolveResult vote(RoomOrchestrator.DissolveVoteCommand command);

    RoomOrchestrator.DissolveResult recover(RoomOrchestrator.DissolveRecoverCommand command);
}
