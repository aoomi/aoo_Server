package com.aoo.bcg.club;
import java.util.Objects;
/** Durable Club projection is changed only after the room authority commits. Retrying the same request repairs either lost acknowledgement. */
public final class ClubRoomKickCoordinator {
 public record Command(String requestId,long clubId,long actorId,long targetPlayerId,long roomId,int actorSeatId,int targetSeatId,String playVersion,long stateVersion) { public Command { if(requestId==null||requestId.isBlank()||clubId<=0||actorId<=0||targetPlayerId<=0||roomId<=0||actorSeatId<0||targetSeatId<0||playVersion==null||playVersion.isBlank()||stateVersion<0)throw new IllegalArgumentException("invalid room kick command"); } }
 public record Receipt(boolean authorityCommitted,long stateVersion,JdbcClubService.State club) {}
 @FunctionalInterface public interface RoomAuthorityPort { long kick(Command command); }
 private final RoomAuthorityPort authority; private final JdbcClubService clubs;
 public ClubRoomKickCoordinator(RoomAuthorityPort authority,JdbcClubService clubs){this.authority=Objects.requireNonNull(authority);this.clubs=Objects.requireNonNull(clubs);}
 public Receipt kick(Command command){long version=authority.kick(command);JdbcClubService.State state=clubs.kickAfterRoomAuthority("room-kick:"+command.requestId(),command.clubId(),command.actorId(),command.targetPlayerId());return new Receipt(true,version,state);}
}
