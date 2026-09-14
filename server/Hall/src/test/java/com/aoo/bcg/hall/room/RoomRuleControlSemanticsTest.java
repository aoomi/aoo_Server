package com.aoo.bcg.hall.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.aoo.bcg.hall.http.HallError;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

final class RoomRuleControlSemanticsTest {
    @Test
    void radioIsRequiredEvenWhenStaleSchemaMarksItOptional() {
        Map<String, Object> schema = Map.of("fields", List.of(Map.of(
                "key", "firstPlayRule", "control", "radio", "required", false,
                "options", List.of(Map.of("value", "winner_first")))));
        assertThrows(HallError.class, () -> RoomRuleSchemaValidator.validate(schema, Map.of()));
    }

    @Test
    void checkboxAcceptsAnEmptySelectionEvenWhenStaleSchemaMarksItRequired() {
        Map<String, Object> schema = Map.of("fields", List.of(Map.of(
                "key", "playRule", "control", "checkbox", "required", true,
                "options", List.of(Map.of("value", "three_no_attachment")))));
        assertEquals(Map.of("playRule", List.of()),
                RoomRuleSchemaValidator.validate(schema, Map.of("playRule", List.of())));
    }
}
