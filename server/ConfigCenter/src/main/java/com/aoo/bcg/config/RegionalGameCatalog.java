package com.aoo.bcg.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RegionalGameCatalog {
    public List<PublishedGameProfile> merge(String provinceCode, String cityCode,
                                             List<PublishedGameProfile> published) {
        if (provinceCode == null || provinceCode.isBlank() || cityCode == null || cityCode.isBlank())
            throw new IllegalArgumentException("normalized province and city codes are required");
        if (published == null) throw new IllegalArgumentException("published profiles are required");
        Map<Long, PublishedGameProfile> selected = new LinkedHashMap<>();
        published.stream().peek(profile -> { if (profile == null) throw new IllegalArgumentException("null catalog profile"); })
                .filter(profile -> visible(profile, provinceCode, cityCode))
                .sorted(Comparator.comparingInt(profile -> priority(profile.scope())))
                .forEach(profile -> selected.put(profile.gameId(), profile));
        return List.copyOf(new ArrayList<>(selected.values()));
    }
    private boolean visible(PublishedGameProfile profile, String provinceCode, String cityCode) {
        return switch (profile.scope()) {
            case NATIONAL -> true;
            case PROVINCE -> profile.provinceCodes().contains(provinceCode);
            case CITY -> profile.provinceCodes().contains(provinceCode) && profile.cityCodes().contains(cityCode);
        };
    }
    private int priority(PublishedGameProfile.Scope scope) {
        return switch (scope) { case NATIONAL -> 0; case PROVINCE -> 1; case CITY -> 2; };
    }
}
