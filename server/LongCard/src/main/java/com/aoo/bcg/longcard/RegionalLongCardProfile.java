package com.aoo.bcg.longcard;

import java.util.Set;

public record RegionalLongCardProfile(String moduleCode, String familyCode,
        Set<LongCardOperation> allowedOperations, int minimumHuPoints, boolean supportsPiaoHua) {
    public RegionalLongCardProfile {
        if (moduleCode == null || moduleCode.isBlank() || familyCode == null || familyCode.isBlank()
                || allowedOperations == null || allowedOperations.isEmpty() || minimumHuPoints < 0)
            throw new IllegalArgumentException("invalid regional long-card profile");
        moduleCode = moduleCode.toUpperCase(java.util.Locale.ROOT);
        allowedOperations = Set.copyOf(allowedOperations);
    }
}
