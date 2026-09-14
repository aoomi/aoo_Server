package com.aoo.bcg.gamespi;

public interface FlowMutation {
    void transition(String expectedState, String nextState);
    void schedule(String timerId, long delayMillis);
    void cancel(String timerId);
}
