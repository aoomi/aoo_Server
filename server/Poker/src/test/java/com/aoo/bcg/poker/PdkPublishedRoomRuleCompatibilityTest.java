package com.aoo.bcg.poker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
  void activeLiangshanSnapshotAcceptsOnlyItsAuditedPreCanonicalIdentity() {
    GameProvider provider = PokerCatalogRuntimeRegistry.providerFor(new GameDescriptor(
        90005, "LS201", "凉山跑得快", GameCategory.POKER, PaoDeKuaiFamily.CODE,
        RegionScope.CITY, "sichuan", "liangshan", "xqp-equivalent-1")).orElseThrow();
    var session = provider.roomFactory().create(new RoomCreationContext(967030, 385, Map.ofEntries(
        Map.entry("playRule", List.of("option_0001", "compare_attachments", "triple_with_one",
            "four_with_two", "four_ace_rank", "all_special_patterns")),
        Map.entry("baseScore", 5),
        Map.entry("payerMode", "OWNER"),
        Map.entry("roundCount", 8),
        Map.entry("jinHuaScore", 1),
        Map.entry("playerCount", 2),
        Map.entry("dealCardCount", 8),
        Map.entry("operationTime", 1000),
        Map.entry("robDealerRule", "dealer_first"),
        Map.entry("roomRestriction", List.of()),
        Map.entry("rule_ls201_0001", List.of("option_0001")))))
        .requireAuthoritativeSession();

    session.execute(new GameCommandRequest("join_req", "legacy-join", 1, 967030, 1,
        "xqp-equivalent-1", "386", 1, Map.of()));
    session.execute(new GameCommandRequest("ready_req", "legacy-owner-ready", 2, 967030, 1,
        "xqp-equivalent-1", "385", 0, Map.of()));
    session.execute(new GameCommandRequest("ready_req", "legacy-player-ready", 3, 967030, 1,
        "xqp-equivalent-1", "386", 1, Map.of()));
    Object activePhase = session.viewFor(385).get("phase");

    Map<String, Object> legacy = new LinkedHashMap<>(session.authoritativeState());
    assertEquals("bb691256808638cedc9ce173f50fafa4305716d659e8ad7ae4cd26f5e37468b3",
        legacy.get("ruleSnapshotKey"));
    legacy.put("ruleSnapshotKey",
        "ea6881545a3448d5ecb4cad7710a80e6238d2d8264b63947f89c4dd68690c843");
    var restored = provider.restoreAuthoritativeSession(legacy).orElseThrow();

    assertEquals(activePhase, restored.viewFor(385).get("phase"));
    assertEquals("bb691256808638cedc9ce173f50fafa4305716d659e8ad7ae4cd26f5e37468b3",
        restored.authoritativeState().get("ruleSnapshotKey"));

    legacy.put("ruleSnapshotKey", "unknown");
    org.junit.jupiter.api.Assertions.assertThrows(IllegalStateException.class,
        () -> provider.restoreAuthoritativeSession(legacy).orElseThrow());
  }

  @Test
  void legacyVerifierAcceptsAReorderedSetButRejectsDifferentSemantics() throws Exception {
    PdkRegionRules regional = new LiangshanPdkRules();
    Map<String, Object> published = Map.of(
        "playerCount", 2,
        "playRule", List.of("compare_attachments", "triple_with_one", "four_with_two",
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
    assertEquals(true, family.matchesLegacyRuleSnapshotKey(legacyKey));
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
        90005, "LS201", "凉山跑得快", GameCategory.POKER, PaoDeKuaiFamily.CODE,
        RegionScope.CITY, "sichuan", "liangshan", "xqp-equivalent-1")).orElseThrow();
    Map<String, Object> published = Map.of(
        "playerCount", 2,
        "rule_ls201_0001", List.of("option_0001"),
        "playRule", List.of("option_0001", "triple_with_one", "compare_attachments"));
    Map<String, Object> normalized = PdkPublishedRuleOptions.normalizePublishedRoomFields(
        published);

    assertEquals(List.of("triple_with_one", "compare_attachments"), normalized.get("playRule"));
    assertFalse(((List<?>) normalized.get("playRule")).contains("option_0001"));

    var session = provider.roomFactory().create(new RoomCreationContext(9000599, 10, published))
        .requireAuthoritativeSession();
    @SuppressWarnings("unchecked")
    Map<String, Object> rules =
        (Map<String, Object>) session.authoritativeState().get("pdkRuleOptions");

    assertEquals("PAIRS", rules.get("tripleAttachmentMode"));
    assertEquals(true, rules.get("compareTripleAttachments"));
    assertEquals("POPUP", rules.get("settlementPresentation"));
    assertEquals(session.authoritativeState(), provider.restoreAuthoritativeSession(
        session.authoritativeState()).orElseThrow().authoritativeState());
  }

  private static String sha256(String value) throws Exception {
    return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256")
        .digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
  }
}
