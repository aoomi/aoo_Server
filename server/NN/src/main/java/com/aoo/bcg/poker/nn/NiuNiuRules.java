package com.aoo.bcg.poker.nn;

import java.util.Map;
import java.util.Set;

/** CN298 权威开房规则快照，只包含牛牛玩法自身规则。 */
public record NiuNiuRules(
    int rounds, int maxPlayers, int startPlayers, Mode mode, int maxRobMultiplier,
    int maxPushMultiplier, StandPolicy standPolicy, boolean fastModeEnabled,
    boolean kanShunDouEnabled) {
  public static final String GAME_CODE = "CN298";
  public static final String FAMILY = "poker:betting";
  public static final String PLAY_VERSION = "cn298-v1.0.0";
  public enum Mode { CLASSIC, PASSION, CRAZY }
  public enum StandPolicy { LOSER_MAY_STAND, EVERYONE_MAY_STAND, NO_ONE_MAY_STAND }

  private static final Set<Integer> ROUND_OPTIONS = Set.of(10, 20, 30);
  private static final Set<Integer> MAX_PLAYER_OPTIONS = Set.of(8, 10);
  private static final Set<Integer> START_PLAYER_OPTIONS = Set.of(2, 4, 6);
  private static final Set<Integer> ROB_OPTIONS = Set.of(3, 4, 5);
  private static final Set<Integer> PUSH_OPTIONS = Set.of(0, 5, 10, 15);

  public NiuNiuRules {
    if (!ROUND_OPTIONS.contains(rounds) || !MAX_PLAYER_OPTIONS.contains(maxPlayers)
        || !START_PLAYER_OPTIONS.contains(startPlayers) || startPlayers > maxPlayers
        || !ROB_OPTIONS.contains(maxRobMultiplier) || !PUSH_OPTIONS.contains(maxPushMultiplier)
        || mode == null || standPolicy == null) throw new IllegalArgumentException("invalid CN298 room rules");
  }

  public static NiuNiuRules defaults() {
    return new NiuNiuRules(10, 8, 2, Mode.CLASSIC, 4, 10,
        StandPolicy.EVERYONE_MAY_STAND, true, true);
  }

  /** XQP 明牌抢庄只公布这三档倍率；0 始终表示不抢。 */
  public Set<Integer> robOptions() {
    return switch (maxRobMultiplier) {
      case 3 -> Set.of(0, 1, 2, 3);
      case 4 -> Set.of(0, 2, 3, 4);
      case 5 -> Set.of(0, 3, 4, 5);
      default -> throw new IllegalStateException("unsupported CN298 rob rule");
    };
  }

  public int settlementMultiplier(NiuNiuHandEvaluator.Type type) {
    int bull = type.legacyCode();
    return switch (mode) {
      case CLASSIC -> Map.ofEntries(
          Map.entry(7,2), Map.entry(8,2), Map.entry(9,3), Map.entry(10,4), Map.entry(11,4),
          Map.entry(101,5), Map.entry(111,5), Map.entry(121,6), Map.entry(131,6),
          Map.entry(141,7), Map.entry(151,8), Map.entry(161,9), Map.entry(171,10)).getOrDefault(bull, 1);
      case PASSION -> bull <= 10 ? Math.max(1, bull) : Map.of(11,10,101,11,111,11,121,12,131,12,141,13,151,14,161,15,171,16).get(bull);
      case CRAZY -> bull <= 10 ? Math.max(1, bull) : Map.of(11,10,101,15,111,15,121,16,131,16,141,17,151,18,161,19,171,20).get(bull);
    };
  }
}
