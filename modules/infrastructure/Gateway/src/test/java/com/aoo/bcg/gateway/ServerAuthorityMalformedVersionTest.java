package com.aoo.bcg.gateway;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ServerAuthorityMalformedVersionTest {
    private final ObjectMapper json = new ObjectMapper();

    @Test void rejectsTheObservedFractionalJsonVersionAndPreviouslyUnexecutedOverflow() throws Exception {
        Map<String,Object> fractional = body("{\"expectedStateVersion\":0.5}");
        assertInstanceOf(Double.class, fractional.get("expectedStateVersion"));
        assertThrows(ServerAuthorityInputGuard.StateVersionConflictException.class,
                () -> ServerAuthorityInputGuard.validate("game.action", fractional, 0));
        Map<String,Object> overflow = body("{\"expectedStateVersion\":18446744073709551616}");
        assertInstanceOf(BigInteger.class, overflow.get("expectedStateVersion"));
        assertThrows(ServerAuthorityInputGuard.StateVersionConflictException.class,
                () -> ServerAuthorityInputGuard.validate("game.action", overflow, 0));
    }

    @Test void rejectsFloatingJsonEvenAfterDecoderRoundingOrAnIntegralResult() throws Exception {
        Map<String,Object> rounded = body("{\"expectedStateVersion\":9007199254740993.0}");
        assertInstanceOf(Double.class, rounded.get("expectedStateVersion"));
        assertEquals(9007199254740992L, ((Number) rounded.get("expectedStateVersion")).longValue());
        assertThrows(ServerAuthorityInputGuard.StateVersionConflictException.class,
                () -> ServerAuthorityInputGuard.validate("game.action", rounded, 9007199254740992L));
        for (String numeric : new String[]{"0.0", "1e0"}) {
            Map<String,Object> parsed = body("{\"expectedStateVersion\":" + numeric + "}");
            long truncated = ((Number) parsed.get("expectedStateVersion")).longValue();
            assertThrows(ServerAuthorityInputGuard.StateVersionConflictException.class,
                    () -> ServerAuthorityInputGuard.validate("game.action", parsed, truncated));
        }
    }

    @Test void permitsSigned64MaximumAndRejectsOverflowOrNegativeJsonValues() throws Exception {
        Map<String,Object> maximum = body("{\"expectedStateVersion\":9223372036854775807}");
        assertInstanceOf(Long.class, maximum.get("expectedStateVersion"));
        assertDoesNotThrow(() -> ServerAuthorityInputGuard.validate("game.action", maximum, Long.MAX_VALUE));
        for (String numeric : new String[]{"9223372036854775808", "-1", "-9223372036854775808"}) {
            Map<String,Object> parsed = body("{\"expectedStateVersion\":" + numeric + "}");
            assertThrows(ServerAuthorityInputGuard.StateVersionConflictException.class,
                    () -> ServerAuthorityInputGuard.validate("fixture.state_req", parsed, 0));
        }
    }

    @Test void acceptsOnlySupportedExactIntegerTypesWithoutFractionOrRangeLoss() {
        for (Number exact : new Number[]{(byte) 3, (short) 3, 3, 3L, new BigInteger("3"), new BigDecimal("3.000")})
            assertDoesNotThrow(() -> ServerAuthorityInputGuard.validate("game.action", Map.of("expectedStateVersion", exact), 3));
        Number unknown = new Number() {
            @Override public int intValue() { return 3; }
            @Override public long longValue() { return 3; }
            @Override public float floatValue() { return 3; }
            @Override public double doubleValue() { return 3; }
        };
        for (Number invalid : new Number[]{new BigDecimal("3.1"), new BigDecimal("18446744073709551616"), 3.0f, 3.0d, Double.NaN, Double.POSITIVE_INFINITY, unknown})
            assertThrows(ServerAuthorityInputGuard.StateVersionConflictException.class,
                    () -> ServerAuthorityInputGuard.validate("game.action", Map.of("expectedStateVersion", invalid), 3));
    }

    @Test void appliesExactValidationToPayloadVersionsAndRejectsNonNumericJsonTypes() throws Exception {
        Map<String,Object> valid = body("{\"payload\":{\"expectedStateVersion\":0}}");
        assertDoesNotThrow(() -> ServerAuthorityInputGuard.validate("game.action", valid, 0));
        for (String malformed : new String[]{"{\"payload\":{\"expectedStateVersion\":0.5}}", "{\"expectedStateVersion\":\"0\"}", "{\"expectedStateVersion\":false}"}) {
            Map<String,Object> parsed = body(malformed);
            assertThrows(ServerAuthorityInputGuard.StateVersionConflictException.class,
                    () -> ServerAuthorityInputGuard.validate("game.action", parsed, 0));
        }
    }

    @Test void retainsLegalOldIntegerStateReadsWhileRejectingMalformedReadVersions() throws Exception {
        Map<String,Object> old = body("{\"expectedStateVersion\":0}");
        assertDoesNotThrow(() -> ServerAuthorityInputGuard.validate("fixture.state_req", old, 1));
        Map<String,Object> dispatchOld = body("{\"expectedStateVersion\":0,\"action\":\"fixture.state_req\"}");
        assertDoesNotThrow(() -> ServerAuthorityInputGuard.validate("fixture.dispatch", dispatchOld, 1));
        Map<String,Object> malformed = body("{\"expectedStateVersion\":0.5}");
        assertThrows(ServerAuthorityInputGuard.StateVersionConflictException.class,
                () -> ServerAuthorityInputGuard.validate("fixture.state_req", malformed, 1));
        Map<String,Object> malformedDispatch = body("{\"expectedStateVersion\":0.5,\"action\":\"fixture.state_req\"}");
        assertThrows(ServerAuthorityInputGuard.StateVersionConflictException.class,
                () -> ServerAuthorityInputGuard.validate("fixture.dispatch", malformedDispatch, 1));
    }

    @Test void preservesMissingVersionAndServerOwnedFieldSemanticsAtTheChangedBoundary() throws Exception {
        Map<String,Object> absent = body("{}");
        assertDoesNotThrow(() -> ServerAuthorityInputGuard.validate("fixture.state_req", absent, 1));
        assertThrows(ServerAuthorityInputGuard.StateVersionConflictException.class,
                () -> ServerAuthorityInputGuard.validate("game.action", absent, 1));
        Map<String,Object> explicitNull = body("{\"expectedStateVersion\":null}");
        assertDoesNotThrow(() -> ServerAuthorityInputGuard.validate("fixture.state_req", explicitNull, 1));
        assertThrows(ServerAuthorityInputGuard.StateVersionConflictException.class,
                () -> ServerAuthorityInputGuard.validate("game.action", explicitNull, 1));
        Map<String,Object> claimed = body("{\"expectedStateVersion\":1,\"payload\":{\"balance\":1}}");
        assertThrows(SecurityException.class,
                () -> ServerAuthorityInputGuard.validate("game.action", claimed, 1));
    }

    private Map<String,Object> body(String text) throws Exception {
        return json.readValue(text, new TypeReference<>() {});
    }
}
