package com.aoo.bcg.poker.nn;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aoo.bcg.gamespi.GameCommandRequest;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CN298AuthorityTest {
  @Test void rejectsCrossRoomAndVersionMismatchBeforeMutation() {
    var authority = authority();
    assertThrows(IllegalArgumentException.class, () -> authority.execute(
        request("poker.cn298.sit_req", "wrong-room", 999, NiuNiuRules.PLAY_VERSION, "1001", 0, Map.of("seatId", 0))));
    assertThrows(IllegalArgumentException.class, () -> authority.execute(
        request("poker.cn298.sit_req", "wrong-version", 298001, "v2", "1001", 0, Map.of("seatId", 0))));
  }

  @Test void rejectsAuthenticatedPlayerUsingAnotherPlayersSeat() {
    var authority = authority();
    authority.execute(request("poker.cn298.sit_req", "sit", 298001, NiuNiuRules.PLAY_VERSION, "1002", 1, Map.of("seatId", 1)));
    assertThrows(IllegalArgumentException.class, () -> authority.execute(
        request("poker.cn298.rob_req", "spoof", 298001, NiuNiuRules.PLAY_VERSION, "1002", 0, Map.of("multiplier", 0))));
  }

  @Test void rejectsProtocolAliasesWithoutCanonicalPrefix() {
    var authority = authority();
    assertThrows(IllegalArgumentException.class, () -> authority.execute(
        request("ready_req", "alias", 298001, NiuNiuRules.PLAY_VERSION, "1001", 0, Map.of())));
  }

  private static CN298Authority authority() {
    return new CN298Authority(new NiuNiuSession(298001, 1001, 1, NiuNiuRules.defaults()), NiuNiuRules.PLAY_VERSION);
  }

  private static GameCommandRequest request(String msgId, String requestId, long roomId,
      String version, String playerId, int seat, Map<String, Object> body) {
    return new GameCommandRequest(msgId, requestId, 1, roomId, 0, version, playerId, seat, body);
  }
}
