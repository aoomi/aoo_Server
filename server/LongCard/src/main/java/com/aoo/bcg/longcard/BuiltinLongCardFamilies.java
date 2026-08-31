package com.aoo.bcg.longcard;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Seven audited regional modules. CP itself is the shared base and is not a game entry. */
public final class BuiltinLongCardFamilies {
    public static final String CHE_PAI = "long-card-che-pai";
    public static final String DA_SI_SHI = "long-card-da-si-shi";
    private static final Set<LongCardOperation> CHE_ACTIONS = Set.of(LongCardOperation.REVEAL,
            LongCardOperation.STEAL, LongCardOperation.DISCARD, LongCardOperation.CHI,
            LongCardOperation.PENG, LongCardOperation.CALL, LongCardOperation.HU, LongCardOperation.PASS);
    private static final Set<LongCardOperation> DSS_ACTIONS = Set.of(LongCardOperation.REVEAL,
            LongCardOperation.DISCARD, LongCardOperation.CHI, LongCardOperation.CALL,
            LongCardOperation.HU, LongCardOperation.PASS);
    private static final Set<LongCardOperation> ZGDSS_ACTIONS = Set.of(LongCardOperation.REVEAL,
            LongCardOperation.DISCARD, LongCardOperation.CHI, LongCardOperation.PENG,
            LongCardOperation.CALL, LongCardOperation.HU, LongCardOperation.PASS);
    private static final Map<String, RegionalLongCardFamily> FAMILIES = create();
    private BuiltinLongCardFamilies() { }

    public static RegionalLongCardFamily require(String moduleCode) {
        RegionalLongCardFamily family = FAMILIES.get(moduleCode == null ? "" : moduleCode.toUpperCase(java.util.Locale.ROOT));
        if (family == null) throw new IllegalArgumentException("unknown long-card module: " + moduleCode);
        return family;
    }
    public static Map<String, RegionalLongCardFamily> all() { return FAMILIES; }

    private static Map<String, RegionalLongCardFamily> create() {
        Map<String, RegionalLongCardFamily> result = new LinkedHashMap<>();
        register(result, "AYCP", CHE_PAI, CHE_ACTIONS, 10, true);
        register(result, "AYDSS", DA_SI_SHI, DSS_ACTIONS, 15, true);
        register(result, "PCDSS", DA_SI_SHI, DSS_ACTIONS, 15, true);
        register(result, "ZGCP", CHE_PAI, CHE_ACTIONS, 14, true);
        register(result, "ZGDSS", DA_SI_SHI, ZGDSS_ACTIONS, 1, true);
        return Map.copyOf(result);
    }
    private static void register(Map<String, RegionalLongCardFamily> target, String module,
            String family, Set<LongCardOperation> operations, int huPoints, boolean piaoHua) {
        target.put(module, new RegionalLongCardFamily(
                new RegionalLongCardProfile(module, family, operations, huPoints, piaoHua)));
    }
}
