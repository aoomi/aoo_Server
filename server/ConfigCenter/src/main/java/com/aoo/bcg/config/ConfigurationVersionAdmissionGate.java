package com.aoo.bcg.config;

import com.aoo.bcg.common.operations.OperationSwitches;

/** A lagging or partially restored node may finish locked rooms but must reject every new room. */
public final class ConfigurationVersionAdmissionGate {
    public void requireNewRoomAllowed(String clusterReleaseId, String localReleaseId, OperationSwitches switches) {
        if (clusterReleaseId == null || clusterReleaseId.isBlank() || localReleaseId == null
                || !clusterReleaseId.equals(localReleaseId))
            throw new IllegalStateException("node configuration version is stale; new rooms are disabled");
        if (switches == null || !switches.allowNewRooms() || switches.maintenance())
            throw new IllegalStateException("play availability switches reject new rooms");
    }
}
