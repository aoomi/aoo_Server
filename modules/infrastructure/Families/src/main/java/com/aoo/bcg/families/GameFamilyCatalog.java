package com.aoo.bcg.families;

import java.util.Map;
import java.util.Set;

/**
 * Production family projection. Historical WordCard remains available only through the
 * explicit Compatibility projection; callers must not treat it as a current family.
 */
public final class GameFamilyCatalog {
    public enum Category { MAHJONG, POKER, LONG_CARD, WORD_CARD, DICE, BOARD, PARTY, OTHER }
    private static final Map<String,Category> BUILTINS=Map.ofEntries(
            Map.entry(BuiltinFamilies.MAHJONG_STANDARD,Category.MAHJONG), Map.entry(BuiltinFamilies.POKER_DOU_DI_ZHU,Category.POKER),
            Map.entry(BuiltinFamilies.LONG_CARD_SICHUAN,Category.LONG_CARD),
            Map.entry(BuiltinFamilies.LONG_CARD_PAO_HU_ZI,Category.LONG_CARD),
            Map.entry("dice-generic",Category.DICE), Map.entry("board-generic",Category.BOARD), Map.entry("party-generic",Category.PARTY));
    private static final Map<String,Category> COMPATIBILITY=Map.of(
            BuiltinFamilies.WORD_CARD_COMPATIBILITY_PAO_HU_ZI, Category.WORD_CARD);
    private GameFamilyCatalog() {}
    public static Category classify(String code){Category result=BUILTINS.get(code);if(result==null)throw new IllegalArgumentException("unclassified family: "+code);return result;}
    public static Map<String,Category> all(){return BUILTINS;}
    /** Resolve a historical family only for an explicitly Compatibility-scoped caller. */
    public static Category classifyCompatibility(String code){Category result=COMPATIBILITY.get(code);if(result==null)throw new IllegalArgumentException("unclassified compatibility family: "+code);return result;}
    public static Map<String,Category> compatibilityFamilies(){return COMPATIBILITY;}
    public static Set<String> independentFamilies(){return Set.of("dice-generic","board-generic","party-generic");}
}
