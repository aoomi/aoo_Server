package com.aoo.bcg.bootstrap;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RoomRuntimeIdentitySerializationContractTest {
    @Test
    void preservesNineteenDigitLifecycleEpochAsCanonicalDecimalString() {
        assertEquals("9223372036854775807",
                JdbcGatewayRoomAuthority.canonicalLifecycleEpoch(Long.MAX_VALUE));
    }

    @Test
    void rejectsNegativeLifecycleEpochInsteadOfEmittingAnInvalidIdentity() {
        assertThrows(IllegalArgumentException.class,
                () -> JdbcGatewayRoomAuthority.canonicalLifecycleEpoch(-1));
    }
}
