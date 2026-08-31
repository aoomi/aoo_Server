package com.aoo.bcg.gamespi;

import java.util.Map;
import java.util.List;
import com.aoo.bcg.gamespi.time.OperationDeadline;
import com.aoo.bcg.gamespi.time.OperationDeadlineArbiter;

/** The only mutable authority used by the unified Gateway path. */
public interface AuthoritativeGameSession {
    GameCommandResult execute(GameCommandRequest request);
    Map<String, Object> viewFor(long viewerPlayerId);
    Map<String, Object> authoritativeState();
    /** Monotonic server-owned revision. It must never be derived from a client sequence. */
    long stateVersion();
    OperationDeadline operationDeadline();
    OperationDeadlineArbiter deadlineArbiter();
    List<String> invariantViolations();
    SettlementPayload settlement(int roundNo, String playVersion);
}
