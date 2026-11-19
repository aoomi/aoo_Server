package com.aoo.bcg.profile;

public interface PlayerProfileRepository {
    PlayerProfileModels.Profile own(long playerId);
    PlayerProfileModels.PublicProfile visible(long viewerId,long playerId);
    PlayerProfileModels.Profile updateProfile(long playerId,PlayerProfileModels.ProfilePatch patch);
    PlayerProfileModels.Profile updatePreferences(long playerId,PlayerProfileModels.PreferencePatch patch);
    PlayerProfileModels.Profile updatePrivacy(long playerId,PlayerProfileModels.PrivacyPatch patch);
    final class NotFound extends RuntimeException { public NotFound(String message){super(message);} }
    final class Conflict extends RuntimeException { public Conflict(String message){super(message);} }
    final class TooFrequent extends RuntimeException { private final long retryAfterSeconds; public TooFrequent(String message,long retry){super(message);retryAfterSeconds=retry;} public long retryAfterSeconds(){return retryAfterSeconds;} }
}
