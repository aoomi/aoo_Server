package com.aoo.bcg.spectator;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class SpectatorPorts {
    private SpectatorPorts() {}
    public record RoomSummary(long roomId,long gameId,String classificationRegionCode,String playVersion,String state,int playerCount) {}
    public record Event(long sequence,String type,Object payload,Instant occurredAt) {}
    public interface RoomPort {
        RoomSummary summary(long roomId);
        boolean mayApprove(long roomId,long playerId);
    }
    public interface ReplayPort { List<Event> publicEvents(long roomId,long afterSequence,Instant visibleBefore,int limit); }
}
