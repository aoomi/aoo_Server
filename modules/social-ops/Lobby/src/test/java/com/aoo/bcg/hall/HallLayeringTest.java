package com.aoo.bcg.hall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class HallLayeringTest {
    @Test void declaresExactlyTheFourHallCapabilities() {
        assertEquals(4, HallApplication.HallCapabilityCatalog.capabilities().size());
    }
}
