package com.aoo.bcg.profile;

/** API models. Media is referenced only by its authoritative asset id. */
public final class PlayerProfileModels {
    private PlayerProfileModels() {}
    public enum Gender { UNSPECIFIED, FEMALE, MALE, NON_BINARY }
    public record Profile(long playerId,String nickname,Long avatarAssetId,Gender gender,String language,
                          boolean soundEnabled,boolean musicEnabled,boolean vibrationEnabled,
                          boolean showGender,long version) {}
    public record PublicProfile(long playerId,String nickname,Long avatarAssetId,Gender gender) {}
    public record ProfilePatch(long expectedVersion,String nickname,Long avatarAssetId,Gender gender) {}
    public record PreferencePatch(long expectedVersion,String language,Boolean soundEnabled,Boolean musicEnabled,Boolean vibrationEnabled) {}
    public record PrivacyPatch(long expectedVersion,Boolean showGender) {}
}
