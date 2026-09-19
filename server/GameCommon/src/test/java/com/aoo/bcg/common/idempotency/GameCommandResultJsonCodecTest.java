package com.aoo.bcg.common.idempotency;

import com.aoo.bcg.gamespi.GameCommandResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class GameCommandResultJsonCodecTest {
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    @Test void readsHistoricalPlainObjectBody() {
        String stored = "{\"msgId\":\"poker.cn298.state_resp\",\"requestId\":\"cn298-1\","
                + "\"body\":{\"phase\":\"WAITING\",\"stateVersion\":0},"
                + "\"serverTimeEpochMillis\":0,\"operationDeadline\":{}}";
        GameCommandResult result = GameCommandResultJsonCodec.decode(json, stored);
        assertEquals("WAITING", result.body().get("phase"));
        assertEquals(0, ((Number) result.body().get("stateVersion")).intValue());
    }
    @Test void currentEncodingRoundTripsThroughTheSameStableSchema() throws Exception {
        var original = new GameCommandResult("state_resp", "request-1", Map.of("phase", "ROBBING"));
        assertEquals(original, GameCommandResultJsonCodec.decode(json, GameCommandResultJsonCodec.encode(json, original)));
    }
    @Test void olderRowsWithoutTimingFieldsRemainReadable() {
        GameCommandResult restored = GameCommandResultJsonCodec.decode(json,
                "{\"msgId\":\"state_resp\",\"requestId\":\"old-1\",\"body\":{\"phase\":\"WAITING\"}}");
        assertEquals(0L, restored.serverTimeEpochMillis()); assertEquals(Map.of(), restored.operationDeadline());
    }
}
