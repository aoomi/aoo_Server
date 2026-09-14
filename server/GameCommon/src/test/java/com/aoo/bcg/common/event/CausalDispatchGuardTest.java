package com.aoo.bcg.common.event;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
class CausalDispatchGuardTest {
    @Test void detectsFeedbackAndDepthButAllowsSequentialDispatch() {
        var guard=new CausalDispatchGuard(2);
        assertThrows(IllegalStateException.class,()->guard.dispatch("store","change-1",
                ()->guard.dispatch("store","change-1",()->{})));
        assertThrows(IllegalStateException.class,()->guard.dispatch("ui","1",
                ()->guard.dispatch("network","2",()->guard.dispatch("store","3",()->{}))));
        guard.dispatch("store","change-1",()->{});
    }
}
