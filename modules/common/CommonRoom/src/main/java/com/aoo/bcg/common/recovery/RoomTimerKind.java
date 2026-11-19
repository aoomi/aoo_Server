package com.aoo.bcg.common.recovery;

/** Non-overlapping lifecycle timers; no timer is reused as another transition's authority. */
public enum RoomTimerKind {
    OPERATION("operation"), DISSOLVE_VOTE("dissolveVote"), SETTLEMENT_DISPLAY("settlementDisplay"),
    CONTINUE_DECISION("continueDecision"), AUTO_READY("autoReady"), NEXT_ROUND("nextRound"),
    INTER_ROUND("interRound"), ROOM_EXPIRATION("roomExpiration");
    private final String key; RoomTimerKind(String key){this.key=key;} public String key(){return key;}
}
