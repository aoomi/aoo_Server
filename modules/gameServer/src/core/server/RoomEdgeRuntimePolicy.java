package core.server;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * One contract for failure semantics around an authoritative room.  Transport
 * delivery is never authority: accepted commands survive disconnects and are
 * recovered from the durable snapshot/event stream.
 */
final class RoomEdgeRuntimePolicy {
    static final Duration MAX_ROOM_AGE = Duration.ofDays(7);
    static final Duration MAX_IDLE_AGE = Duration.ofHours(6);
    static final Duration LEASE_TTL = Duration.ofMinutes(2);

    enum Availability { AVAILABLE, DEGRADED, UNAVAILABLE }
    enum BanDecision { FINISH_CURRENT_ROUND, REJECT_NEXT_ROUND }
    enum RetirementDecision { CONTINUE_PINNED_ROOM, REJECT_NEW_ROUND, REPLAY_ALLOWED }

    interface FaultInjector {
        FaultInjector NONE = point -> { };
        void at(String point);
    }

    record Dependencies(Availability database, Availability redis, Availability messageQueue) {
        Dependencies {
            Objects.requireNonNull(database); Objects.requireNonNull(redis); Objects.requireNonNull(messageQueue);
        }
        boolean acceptsMutation() { return database != Availability.UNAVAILABLE; }
        boolean singleAuthoritySafe() { return redis != Availability.UNAVAILABLE; }
        boolean confirmationDeferred() { return messageQueue != Availability.AVAILABLE; }
    }

    record Lifecycle(Instant createdAt, Instant lastActivityAt, boolean roundInProgress,
                     boolean playerBanned, boolean playRetired) {
        Lifecycle { Objects.requireNonNull(createdAt); Objects.requireNonNull(lastActivityAt); }
    }

    record Decision(boolean acceptMutation, boolean retainSeat, boolean deferBroadcast,
                    boolean allowTakeover, boolean allowNextRound, String reason) { }

    private final FaultInjector faults;

    RoomEdgeRuntimePolicy(FaultInjector faults) { this.faults = Objects.requireNonNull(faults); }
    static RoomEdgeRuntimePolicy production() { return new RoomEdgeRuntimePolicy(FaultInjector.NONE); }

    void validateIdentity(long roomId, int roundNo) {
        if (roomId <= 0 || roomId == Long.MAX_VALUE) throw new IllegalArgumentException("roomId outside durable range");
        if (roundNo < 0 || roundNo == Integer.MAX_VALUE) throw new IllegalArgumentException("roundNo outside durable range");
    }

    void validateCommandIdentity(long authoritativeRoomId, long requestRoomId, int roundNo) {
        validateIdentity(authoritativeRoomId, roundNo);
        if (authoritativeRoomId != requestRoomId) throw new IllegalArgumentException("request roomId does not match authority");
    }

    void validateRoomIdentity(long roomId) {
        if (roomId <= 0 || roomId == Long.MAX_VALUE) throw new IllegalArgumentException("roomId outside durable range");
    }

    Decision decide(Dependencies dependencies, Lifecycle lifecycle, Instant now) {
        Objects.requireNonNull(now);
        boolean expired = Duration.between(lifecycle.createdAt(), now).compareTo(MAX_ROOM_AGE) > 0
                || (!lifecycle.roundInProgress()
                && Duration.between(lifecycle.lastActivityAt(), now).compareTo(MAX_IDLE_AGE) > 0);
        boolean next = !expired && !lifecycle.playerBanned() && !lifecycle.playRetired();
        boolean accept = dependencies.acceptsMutation() && dependencies.singleAuthoritySafe() && !expired;
        String reason = expired ? "ROOM_EXPIRED" : lifecycle.playerBanned() ? "PLAYER_BANNED"
                : lifecycle.playRetired() ? "PLAY_RETIRED" : !dependencies.acceptsMutation() ? "DATABASE_UNAVAILABLE"
                : !dependencies.singleAuthoritySafe() ? "AUTHORITY_STORE_UNAVAILABLE" : "OK";
        return new Decision(accept, lifecycle.roundInProgress(), dependencies.confirmationDeferred(),
                dependencies.singleAuthoritySafe(), next, reason);
    }

    BanDecision banDecision(boolean roundInProgress) {
        return roundInProgress ? BanDecision.FINISH_CURRENT_ROUND : BanDecision.REJECT_NEXT_ROUND;
    }

    RetirementDecision retirementDecision(boolean existingRoom, boolean startingNextRound) {
        if (!existingRoom) return RetirementDecision.REPLAY_ALLOWED;
        return startingNextRound ? RetirementDecision.REJECT_NEW_ROUND : RetirementDecision.CONTINUE_PINNED_ROOM;
    }

    String accountingDay(Instant instant, ZoneId businessZone) {
        return ZonedDateTime.ofInstant(instant, businessZone).toLocalDate().toString();
    }

    void fault(String point) { faults.at(point); }

    static boolean retryable(Throwable failure) {
        for (Throwable current=failure; current!=null; current=current.getCause()) {
            if (current instanceof java.sql.SQLTransientException
                    || current instanceof java.sql.SQLRecoverableException) return true;
            if (current.getCause()==current) break;
        }
        return false;
    }
}
