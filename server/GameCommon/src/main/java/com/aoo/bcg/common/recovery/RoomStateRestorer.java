package com.aoo.bcg.common.recovery;

import java.util.List;

public interface RoomStateRestorer<S, E> {
    S fromSnapshot(RoomSnapshot snapshot);
    S replay(S state, List<E> events);
    String digest(S state);
}
