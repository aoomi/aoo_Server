package com.aoo.bcg.gamespi;

import java.util.Objects;

public record GameDescriptor(
        int gameId,
        String code,
        String displayName,
        GameCategory category,
        String family,
        RegionScope regionScope,
        String provinceCode,
        String cityCode,
        String version) {

    public GameDescriptor {
        if (gameId <= 0) throw new IllegalArgumentException("gameId must be positive");
        code = requireText(code, "code").toLowerCase();
        displayName = requireText(displayName, "displayName");
        category = Objects.requireNonNull(category, "category");
        family = requireText(family, "family");
        regionScope = Objects.requireNonNull(regionScope, "regionScope");
        version = requireText(version, "version");
        provinceCode = normalize(provinceCode);
        cityCode = normalize(cityCode);
        if (regionScope == RegionScope.NATIONAL && (!provinceCode.isEmpty() || !cityCode.isEmpty())) {
            throw new IllegalArgumentException("national game cannot bind province or city");
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " must not be blank");
        return value.trim();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
