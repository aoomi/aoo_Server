package com.aoo.bcg.common.invite;
/** Server-side invite admission; a valid signature never bypasses room access rules. */
public final class InviteAdmissionPolicy{
 public record Context(boolean roomExists,boolean roomJoinable,boolean privateRoom,boolean privateAccess,
  long requiredClubId,boolean clubMember,boolean tournamentRoom,boolean tournamentEligible,
  boolean spectatorRequested,boolean spectatorAllowed,boolean blacklisted,boolean versionCompatible){}
 public record Decision(boolean allowed,String code){public static Decision allow(){return new Decision(true,"OK");}public static Decision deny(String code){return new Decision(false,code);}}
 public Decision decide(Context value){if(!value.roomExists())return Decision.deny("ROOM_NOT_FOUND");if(value.blacklisted())return Decision.deny("BLACKLISTED");if(!value.versionCompatible())return Decision.deny("VERSION_INCOMPATIBLE");if(!value.roomJoinable())return Decision.deny("ROOM_NOT_JOINABLE");if(value.privateRoom()&&!value.privateAccess())return Decision.deny("PRIVATE_ACCESS_REQUIRED");if(value.requiredClubId()>0&&!value.clubMember())return Decision.deny("CLUB_MEMBERSHIP_REQUIRED");if(value.tournamentRoom()&&!value.tournamentEligible())return Decision.deny("TOURNAMENT_INELIGIBLE");if(value.spectatorRequested()&&!value.spectatorAllowed())return Decision.deny("SPECTATOR_NOT_ALLOWED");return Decision.allow();}
}
