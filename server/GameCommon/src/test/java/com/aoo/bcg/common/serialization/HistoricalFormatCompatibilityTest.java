package com.aoo.bcg.common.serialization;

import com.aoo.bcg.common.event.EventSchemaRegistry;
import com.aoo.bcg.common.config.RoomRuleSnapshot;
import com.aoo.bcg.common.recovery.RoomDeadlineSnapshot;
import com.aoo.bcg.common.recovery.RoomSnapshot;
import com.aoo.bcg.common.replay.ReplayFrame;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HistoricalFormatCompatibilityTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test void readsAndUpcastsHistoricalEventJsonWithoutDroppingUnknownData() throws Exception {
        var schemas = new EventSchemaRegistry()
                .register("poker.played", 2)
                .upcaster("poker.played", 1, node -> {
                    ObjectNode value = (ObjectNode) node.deepCopy();
                    value.set("cardList", value.remove("cards"));
                    value.put("schemaVersion", 2);
                    return value;
                });
        var historical = mapper.readTree("{\"cards\":[3,4],\"legacyMarker\":\"keep\"}");
        var current = schemas.upcast("poker.played", 1, historical);
        assertEquals(2, current.path("schemaVersion").asInt());
        assertEquals(2, current.path("cardList").size());
        assertEquals("keep", current.path("legacyMarker").asText());
        assertThrows(IllegalArgumentException.class, () -> schemas.upcast("poker.played", 3, historical));
    }

    @Test void restoresHistoricalSnapshotDeadlineAndReplayValueTypes() {
        var snapshot = new RoomSnapshot(7, 62, "njpdk-v1", "legacy-2.22", 9, 4, 4,
                Instant.EPOCH, Map.of("turn", 2, "cards", java.util.List.of(3, 4)));
        var deadlines = RoomDeadlineSnapshot.fromState(Map.of("deadlines", Map.of(
                "operation", 1L, "dissolveVote", 2L, "interRound", 3L, "roomExpiration", 4L)));
        var replay = new ReplayFrame(4, Instant.EPOCH, "poker.played", Map.of("cards", java.util.List.of(3, 4)));
        assertEquals("legacy-2.22", snapshot.componentVersion());
        assertEquals(4, snapshot.lastEventSequence());
        assertEquals(RoomDeadlineSnapshot.REQUIRED.size(), deadlines.deadlineEpochMillis().size());
        assertEquals(java.util.List.of(3, 4), ((Map<?, ?>) replay.publicPayload()).get("cards"));
    }

    @Test void preservesHistoricalRuleConfigurationAndUnknownOptions() throws Exception {
        String json = "{\"configId\":22,\"gameId\":62,\"playVersion\":\"njpdk-v1\",\"componentVersion\":\"legacy-2.22\",\"roomProfileVersion\":\"room-v1\",\"flowVersion\":\"flow-v1\",\"scoreVersion\":\"score-v1\",\"uiProfileVersion\":\"ui-v1\",\"publishedAt\":\"1970-01-01T00:00:00Z\",\"immutableRules\":{\"playerNum\":3,\"legacySpecialRule\":true}}";
        var configuredMapper = new ObjectMapper().findAndRegisterModules();
        var root = configuredMapper.readTree(json);
        @SuppressWarnings("unchecked") Map<String,Object> rules = configuredMapper.convertValue(root.path("immutableRules"), Map.class);
        RoomRuleSnapshot snapshot = new RoomRuleSnapshot(root.path("configId").asLong(), root.path("gameId").asInt(),
                root.path("playVersion").asText(), root.path("componentVersion").asText(),
                root.path("roomProfileVersion").asText(), root.path("flowVersion").asText(),
                root.path("scoreVersion").asText(), root.path("uiProfileVersion").asText(),
                Instant.parse(root.path("publishedAt").asText()), rules);
        assertEquals(3, ((Number) snapshot.immutableRules().get("playerNum")).intValue());
        assertEquals(Boolean.TRUE, snapshot.immutableRules().get("legacySpecialRule"));
        assertEquals("legacy-2.22", snapshot.componentVersion());
    }
}
