package com.aoo.bcg.gamespi;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StrictEnumDecoderTest {
    private enum Wire { ZERO, ONE }
    @Test void acceptsExactKnownNamesAndRejectsUnknownNames() {
        assertEquals(Wire.ONE, StrictEnumDecoder.byName(Wire.class, "ONE"));
        assertThrows(IllegalArgumentException.class, () -> StrictEnumDecoder.byName(Wire.class, "one"));
        assertThrows(IllegalArgumentException.class, () -> StrictEnumDecoder.byName(Wire.class, "FUTURE"));
    }
    @Test void rejectsUnknownNumericCodesInsteadOfReturningDefault() {
        assertEquals(Wire.ONE, StrictEnumDecoder.byCode(Wire.class, 1, Enum::ordinal));
        assertThrows(IllegalArgumentException.class, () -> StrictEnumDecoder.byCode(Wire.class, 99, Enum::ordinal));
    }
}
