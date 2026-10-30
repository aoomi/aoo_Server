package com.aoo.bcg.common.operations;

public record OperationSwitches(boolean allowNewRooms, boolean allowExistingRoomsToFinish,
                                boolean maintenance, String minimumClientVersion,
                                String rolloutKey, String serverRouteVersion) {
    public OperationSwitches {
        if (minimumClientVersion == null || minimumClientVersion.isBlank()
                || serverRouteVersion == null || serverRouteVersion.isBlank())
            throw new IllegalArgumentException("version switches are required");
        if (maintenance && allowNewRooms) throw new IllegalArgumentException("maintenance cannot allow new rooms");
    }
}
