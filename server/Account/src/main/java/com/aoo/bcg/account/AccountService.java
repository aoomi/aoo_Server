package com.aoo.bcg.account;

/** Read-only account projection port used by hall consumers. Security mutations have one owner: {@link AccountSecurityService}. */
public interface AccountService {
    PlayerProfile profile(long accountId);

    record PlayerProfile(long accountId, String displayName, String avatarUrl, long revision) {
        public PlayerProfile {
            if (accountId <= 0 || displayName == null || revision < 0) {
                throw new IllegalArgumentException("invalid player profile");
            }
        }
    }
}
