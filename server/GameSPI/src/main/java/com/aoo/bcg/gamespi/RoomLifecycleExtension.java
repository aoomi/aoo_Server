package com.aoo.bcg.gamespi;

public interface RoomLifecycleExtension {
    default void onCreated(RoomReadView room) {}
    default void onStarted(RoomReadView room) {}
    default void onRoundEnded(RoomReadView room) {}
    default void onDissolved(RoomReadView room) {}
    default void onRecovered(RoomReadView room) {}
}
