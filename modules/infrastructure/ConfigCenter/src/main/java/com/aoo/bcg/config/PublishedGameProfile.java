package com.aoo.bcg.config;

import com.aoo.bcg.common.config.RoomRuleSnapshot;
import com.aoo.bcg.common.operations.OperationSwitches;
import java.time.Instant;
import java.util.Set;

public record PublishedGameProfile(long gameId, String version, Scope scope, Set<String> provinceCodes,
                                   Set<String> cityCodes, RoomRuleSnapshot roomSnapshot,
                                   OperationSwitches switches, Instant publishedAt) {
    public enum Scope { NATIONAL, PROVINCE, CITY }
    public PublishedGameProfile {
        provinceCodes = Set.copyOf(provinceCodes == null ? Set.of() : provinceCodes);
        cityCodes = Set.copyOf(cityCodes == null ? Set.of() : cityCodes);
        if (gameId <= 0 || version == null || version.isBlank() || scope == null || roomSnapshot == null
                || switches == null || publishedAt == null) throw new IllegalArgumentException("incomplete game profile");
        if (scope == Scope.NATIONAL && (!provinceCodes.isEmpty() || !cityCodes.isEmpty()))
            throw new IllegalArgumentException("national profiles must not be copied to regions");
        if (scope == Scope.PROVINCE && (provinceCodes.isEmpty() || !cityCodes.isEmpty()))
            throw new IllegalArgumentException("province profiles require provinces and forbid cities");
        if (scope == Scope.CITY && (provinceCodes.isEmpty() || cityCodes.isEmpty()))
            throw new IllegalArgumentException("city profiles require province and city scope");
        if (roomSnapshot.gameId() != gameId || !roomSnapshot.playVersion().equals(version))
            throw new IllegalArgumentException("profile identity must match its locked room snapshot");
    }
}
