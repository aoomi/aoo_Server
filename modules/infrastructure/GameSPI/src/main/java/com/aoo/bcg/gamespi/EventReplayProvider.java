package com.aoo.bcg.gamespi;

import java.util.List;

/** Version-pinned reducer used only while rebuilding an authority after takeover. */
@FunctionalInterface
public interface EventReplayProvider {
    StatePayload replay(StatePayload snapshot, List<Object> orderedEvents);
}
