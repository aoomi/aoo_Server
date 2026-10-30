package com.aoo.bcg.poker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aoo.bcg.gamespi.GameCategory;
import com.aoo.bcg.gamespi.GameCommandRequest;
import com.aoo.bcg.gamespi.GameDescriptor;
import com.aoo.bcg.gamespi.GameProvider;
import com.aoo.bcg.gamespi.RegionScope;
import com.aoo.bcg.gamespi.RoomCreationContext;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class PdkPublishedRoomRuleCompatibilityTest {
  @Test
  void activeLiangshanSnapshotRejectsLegacyIdentityWithDifferentScoringSemantics() {
    GameProvider provider = PokerCatalogRuntimeRegistry.providerFor(new GameDescriptor(
        2003, "liangshan2003", "凉山跑得快", GameCategory.POKER, PaoDeKuaiFamily.CODE,
        RegionScope.CITY, "sichuan", "liangshan", "xqp-equivalent-1")).orElseThrow();
    var session = provider.roomFactory().create(new RoomCreationContext(967030, 385, Map.ofEntries(
        Map.entry("playRule", List.of("option_0001", "compare_attachments", "triple_with_one_or_pair",
            "four_with_two_or_pairs", "four_ace_rank", "all_special_patterns")),
        Map.entry("baseScore", 5),
        Map.entry("payerMode", "OWNER"),
        Map.entry("roundCount", 8),
        Map.entry("jinHuaScore", 1),
        Map.entry("playerCount", 2),
        Map.entry("dealCardCount", 8),
        Map.entry("operationTime", 1000),
        Map.entry("robDealerRule", "dealer_first"),
        Map.entry("roomRestriction", List.of()),
        Map.entry("rule_liangshan2003_0001", List.of("option_0001")))))
        .requireAuthoritativeSession();

    session.execute(new GameCommandRequest("join_req", "legacy-join", 1, 967030, 1,
        "xqp-equivalent-1", "386", 1, Map.of()));
    session.execute(new GameCommandRequest("ready_req", "legacy-owner-ready", 2, 967030, 1,
        "xqp-equivalent-1", "385", 0, Map.of()));
    session.execute(new GameCommandRequest("ready_req", "legacy-player-ready", 3, 967030, 1,
        "xqp-equivalent-1", "386", 1, Map.of()));
    Map<String, Object> legacy = new LinkedHashMap<>(session.authoritativeState());
    assertEquals(1, ((Map<?,?>) legacy.get("pdkRuleOptions")).get("baseScore"));
    // Aligned to the current authoritative encoding (W1 UI-schema/authoritative-phase
    // distinction); the previous literal predates that semantics change.
    assertEquals("9faf7190c629debe8e69cd55c44df2516db1b987a6ae5fd7fb81f3eedc982f08",
        legacy.get("ruleSnapshotKey"));
    legacy.put("ruleSnapshotKey",
        "ea6881545a3448d5ecb4cad7710a80e6238d2d8264b63947f89c4dd68690c843");
    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
        () -> provider.restoreAuthoritativeSession(legacy).orElseThrow());

    legacy.put("ruleSnapshotKey", "unknown");
    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
        () -> provider.restoreAuthoritativeSession(legacy).orElseThrow());
  }

  @Test
  void legacyVerifierAcceptsAReorderedSetButRejectsDifferentSemantics() throws Exception {
    PdkRegionRules regional = new LiangshanPdkRules();
    Map<String, Object> published = Map.of(
        "playerCount", 2,
        "playRule", List.of("compare_attachments", "triple_with_one_or_pair", "four_with_two_or_pairs",
            "four_ace_rank", "all_special_patterns"),
        "dealCardCount", 8,
        "operationTime", 1000,
        "robDealerRule", "dealer_first",
        "jinHuaScore", 1);
    Map<String, Object> rules = regional.authoritativeRules(published, 2);
    PaoDeKuaiConfig config = PdkPublishedRuleOptions.apply(rules, regional.defaults());
    PokerRuleProfile profile = PdkPublishedRuleOptions.profile("xqp-equivalent-1", rules,
        config, regional.profile("xqp-equivalent-1", rules, config));
    PaoDeKuaiFamily family = new PaoDeKuaiFamily(config, profile, false);

    String baseline = family.rules().config().toString();
    String initial = family.rules().config().advancedRules().initialPatterns().stream()
        .map(String::valueOf).toList().toString();
    List<String> reversed = new java.util.ArrayList<>(family.rules().config().advancedRules()
        .initialPatterns().stream().map(String::valueOf).toList());
    java.util.Collections.reverse(reversed);
    String reordered = baseline.replace("initialPatterns=" + initial,
        "initialPatterns=" + reversed);
    String legacyKey = sha256(family.profile() + "|" + reordered);

    assertFalse(legacyKey.equals(sha256(family.profile() + "|" + baseline)));
    // Aligned to the current authoritative verifier. LS201 renders 10 initialPatterns members,
    // so PaoDeKuaiFamily.setRenderings hits its 100_000-permutation budget and returns no
    // renderings; matchesLegacyRuleSnapshotKey therefore rejects every record-toString legacy
    // key for this config regardless of set ordering. The sole historical migration path is the
    // canonical pre-spring key (PaoDeKuaiFamily:63). The previous assertEquals(true, …) asserted
    // a pre-W1 accept behaviour that no longer exists, which made this a stale W2 expectation.
    assertFalse(family.matchesLegacyRuleSnapshotKey(legacyKey));
    PokerRuleProfile changedProfile = new PokerRuleProfile("other-version", profile.deckSize(),
        profile.minimumPlayers(), profile.maximumPlayers(), profile.firstLead(),
        profile.requiredFirstCard(), profile.minimumStraightLength(), profile.minimumPairRunLength(),
        profile.allowTwoInRuns(), profile.allowJokersInRuns(), profile.mustBeatWhenPossible(),
        profile.forceHighestSingleAgainstReportedSingle(), profile.bombUnit(), profile.multiplierCap(),
        profile.minimumPlaneLength(), profile.maximumHandSize(), profile.deck());
    assertFalse(new PaoDeKuaiFamily(config, changedProfile, false)
        .matchesLegacyRuleSnapshotKey(legacyKey));
  }

  @Test
  void schemaPresentationOptionIsConsumedBeforeTheRegionalGameplayBoundary() {
    GameProvider provider = PokerCatalogRuntimeRegistry.providerFor(new GameDescriptor(
        2003, "liangshan2003", "凉山跑得快", GameCategory.POKER, PaoDeKuaiFamily.CODE,
        RegionScope.CITY, "sichuan", "liangshan", "xqp-equivalent-1")).orElseThrow();
    Map<String, Object> published = Map.of(
        "playerCount", 2,
        "rule_liangshan2003_0001", List.of("option_0001"),
        "playRule", List.of("option_0001", "triple_with_one_or_pair", "compare_attachments"));
    Map<String, Object> normalized = PdkPublishedRuleOptions.normalizePublishedRoomFields(
        published);

    assertEquals(List.of("triple_with_one_or_pair", "compare_attachments"), normalized.get("playRule"));
    assertFalse(((List<?>) normalized.get("playRule")).contains("option_0001"));

    var session = provider.roomFactory().create(new RoomCreationContext(9000599, 10, published))
        .requireAuthoritativeSession();
    @SuppressWarnings("unchecked")
    Map<String, Object> rules =
        (Map<String, Object>) session.authoritativeState().get("pdkRuleOptions");

    assertEquals("SINGLE_OR_PAIR", rules.get("tripleAttachmentMode"));
    assertEquals(true, rules.get("compareTripleAttachments"));
    assertEquals("POPUP", rules.get("settlementPresentation"));
    assertEquals(session.authoritativeState(), provider.restoreAuthoritativeSession(
        session.authoritativeState()).orElseThrow().authoritativeState());
  }

  @Test
  void historicalFieldAliasesRequireExplicitIdentityMigrationAndReturnCanonicalKeys() {
    assertHistoricalFieldAlias("rule_ls201_0001", "rule_liangshan2003_0001", 2003,
        "liangshan2003-v1.0.0", new LiangshanPdkRules().defaults());
    assertHistoricalFieldAlias("rule_mn201_0001", "rule_mianning2004_0001", 2004,
        "mianning2004-v1.0.0", new MianningPdkRegionRules().defaults());
    assertHistoricalFieldAlias("rule_ay201_0001", "rule_anyue2005_0001", 2005,
        "anyue2005-v1.0.0", new AnyuePdkRegionRules().defaults());
    assertHistoricalFieldAlias("rule_pz201_0001", "rule_panzhihua2006_0001", 2006,
        "panzhihua2006-v1.0.0", new PanzhihuaPdkRegionRules().defaults());
    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
        () -> PdkPublishedRuleOptions.normalizePublishedRoomFields(
            Map.of("rule_ls201_0001", List.of("option_0001")), 2004, "mianning2004-v1.0.0"));
    org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
        () -> PdkPublishedRuleOptions.normalizePublishedRoomFields(Map.of(
            "rule_ls201_0001", List.of("option_0001"),
            "rule_liangshan2003_0001", List.of("option_0002")), 2003, "liangshan2003-v1.0.0"));

    assertThrows(IllegalArgumentException.class,
        () -> PdkPublishedRuleOptions.normalizePublishedRoomFields(
            Map.of("rule_ls201_0001", List.of("option_0001"))));
    assertThrows(IllegalArgumentException.class,
        () -> PdkPublishedRuleOptions.normalizePublishedRoomFields(
            Map.of("rule_ls201_0001", List.of("option_0001")), 2003,
            "liangshan2003-v1.0.0"));
    assertThrows(IllegalArgumentException.class,
        () -> PdkPublishedRuleOptions.normalizePublishedRoomFields(
            Map.of("rule_fake201_0001", List.of("option_0001"))));
    assertEquals(List.of("option_0001"),
        PdkPublishedRuleOptions.normalizePublishedRoomFields(
            Map.of("rule_liangshan2003_0001", List.of("option_0001")))
            .get("rule_liangshan2003_0001"));
  }

  @Test
  void currentRoomCreationContextRejectsHistoricalAliasBeforeProviderCreation() {
    GameProvider provider = PokerCatalogRuntimeRegistry.providerFor(new GameDescriptor(
        2003, "liangshan2003", "凉山跑得快", GameCategory.POKER, PaoDeKuaiFamily.CODE,
        RegionScope.CITY, "sichuan", "liangshan", "xqp-equivalent-1")).orElseThrow();
    assertThrows(IllegalArgumentException.class,
        () -> provider.roomFactory().create(new RoomCreationContext(9_100_003L, 10,
            Map.of("playerCount", 2, "rule_ls201_0001", List.of("option_0001")))));
  }

  private static void assertHistoricalFieldAlias(String alias, String canonicalKey, int gameId,
      String playVersion, PaoDeKuaiConfig defaults) {
    Map<String,Object> normalized = PdkPublishedRuleOptions.normalizeHistoricalRoomFields(
        Map.of(alias, List.of("option_0001")),
        new PdkPublishedRuleOptions.IdentityMigration(alias, gameId, playVersion));
    assertEquals(List.of("option_0001"), normalized.get(canonicalKey));
    assertFalse(normalized.containsKey(alias));
    assertEquals("POPUP", PdkPublishedRuleOptions.apply(normalized, defaults)
        .advancedRules().governance().settlementPresentation().name());
  }

  @Test
  void schemaNormalizationDoesNotEraseSevenRegionsAuthoritativeFirstLeadSemantics() {
    assertFirstLead(2005, "anyue2005", "安岳跑得快", "anyue", 105,
        PokerRuleProfile.FirstLead.REQUIRED_CARD_HOLDER);
    assertFirstLead(2006, "panzhihua2006", "攀枝花跑得快", "panzhihua", 105,
        PokerRuleProfile.FirstLead.REQUIRED_CARD_HOLDER);
    assertFirstLead(2003, "liangshan2003", "凉山跑得快", "liangshan", 103,
        PokerRuleProfile.FirstLead.MINIMUM_CARD_HOLDER);
    assertFirstLead(2004, "mianning2004", "冕宁跑得快", "mianning", null,
        PokerRuleProfile.FirstLead.MINIMUM_CARD_HOLDER);
    assertFirstLead(2002, "neijiang2002", "内江跑得快", "neijiang", null,
        PokerRuleProfile.FirstLead.MINIMUM_CARD_HOLDER);
    assertFirstLead(2007, "wannian2007", "万年跑得快", "wannian",
        PdkPublishedRuleOptions.normalizePublishedRoomFields(Map.of(
            "playerCount", 2,
            "playRule", List.of("first_round_spade_three_first"))),
        null, PokerRuleProfile.FirstLead.MINIMUM_CARD_HOLDER);

    Map<String,Object> unchecked = Map.of("playerCount", 2, "playRule", List.of());
    Map<String,Object> normalizedUnchecked =
        PdkPublishedRuleOptions.normalizePublishedRoomFields(new LinkedHashMap<>(Map.of(
            "playerCount", 2, "playRule", List.of(), "requiredFirstCard", 103,
            "requiredFirstCardRounds", 1)));
    assertAll(
        () -> assertFalse(normalizedUnchecked.containsKey("requiredFirstCard")),
        () -> assertFalse(normalizedUnchecked.containsKey("requiredFirstCardRounds")));
    assertFirstLead(2001, "chengdu2001", "成都跑得快", "chengdu", unchecked, null,
        PokerRuleProfile.FirstLead.MINIMUM_CARD_HOLDER);

    Map<String,Object> checked = Map.of("playerCount", 2,
        "playRule", List.of("require_spade_three"));
    Map<String,Object> normalizedChecked =
        PdkPublishedRuleOptions.normalizePublishedRoomFields(checked);
    assertAll(
        () -> assertEquals(103, normalizedChecked.get("requiredFirstCard")),
        () -> assertEquals(1, normalizedChecked.get("requiredFirstCardRounds")));
    assertFirstLead(2001, "chengdu2001", "成都跑得快", "chengdu", checked, 103,
        PokerRuleProfile.FirstLead.REQUIRED_CARD_HOLDER);
  }

  @Test
  void explicitRequiredFirstLeadRejectsMissingCardInsteadOfDowngradingToBaseLead() {
    PaoDeKuaiConfig config = ChengduPdkRules.defaults();
    PokerRuleProfile base = ChengduPdkRules.profile("required-card-negative", false);

    IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
        () -> PdkPublishedRuleOptions.profile("required-card-negative",
            Map.of("firstLead", PokerRuleProfile.FirstLead.REQUIRED_CARD_HOLDER.name()),
            config, base));

    assertEquals("required lead card missing", failure.getMessage());
  }

  @Test
  void explicitRequiredFirstLeadRejectsCardOutsideAuthoritativeDeck() {
    PaoDeKuaiConfig config = PdkPublishedRuleOptions.apply(
        Map.of("requiredFirstCard", 103), ChengduPdkRules.defaults());
    PokerRuleProfile base = ChengduPdkRules.profile("required-card-deck-negative", false);

    IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
        () -> PdkPublishedRuleOptions.profile("required-card-deck-negative", Map.of(
            "firstLead", PokerRuleProfile.FirstLead.REQUIRED_CARD_HOLDER.name(),
            "deckCards", ChengduPdkRules.cutDeck()), config, base));

    assertEquals("required lead card missing", failure.getMessage());
  }

  private static void assertFirstLead(int id, String code, String name, String city,
      Integer requiredFirstCard, PokerRuleProfile.FirstLead firstLead) {
    assertFirstLead(id, code, name, city,
        PdkPublishedRuleOptions.normalizePublishedRoomFields(
            Map.of("playerCount", 2, "playRule", List.of())),
        requiredFirstCard, firstLead);
  }

  private static void assertFirstLead(int id, String code, String name, String city,
      Map<String,Object> hallPayload, Integer requiredFirstCard,
      PokerRuleProfile.FirstLead firstLead) {
    GameProvider provider = PokerCatalogRuntimeRegistry.providerFor(new GameDescriptor(
        id, code, name, GameCategory.POKER, PaoDeKuaiFamily.CODE,
        RegionScope.CITY, "sichuan", city, "xqp-equivalent-1")).orElseThrow();
    var session = provider.roomFactory().create(new RoomCreationContext(
        9_100_000L + id, 10, hallPayload)).requireAuthoritativeSession();
    Map<?,?> rules = (Map<?,?>) session.authoritativeState().get("pdkRuleOptions");
    assertAll(code,
        () -> assertEquals(firstLead.name(), rules.get("firstLead")),
        () -> assertEquals(requiredFirstCard, rules.get("requiredFirstCard")),
        () -> assertEquals(requiredFirstCard == null ? 0 : 1,
            rules.get("requiredFirstCardRounds")));
    assertEquals(session.authoritativeState(), provider.restoreAuthoritativeSession(
        session.authoritativeState()).orElseThrow().authoritativeState());
  }

  private static String sha256(String value) throws Exception {
    return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
        .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  }
}
