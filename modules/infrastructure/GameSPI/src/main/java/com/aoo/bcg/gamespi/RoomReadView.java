package com.aoo.bcg.gamespi;

import java.util.Map;

public interface RoomReadView {
    long roomId();
    int gameId();
    String gameVersion();
    RoomState state();
    Map<String, Object> immutableRules();
}
