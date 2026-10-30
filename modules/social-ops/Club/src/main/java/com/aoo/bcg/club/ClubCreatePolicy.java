package com.aoo.bcg.club;

/** Trusted immutable action policy; canonical publication verifies its content hash. */
public record ClubCreatePolicy(String currency, boolean eligibilityEnabled, long minimumRoomCardBalance,
        boolean chargeEnabled, long chargeRoomCardAmount, int policyVersion, String contentHash) {
    public ClubCreatePolicy {
        if (!"ROOM_CARD".equals(currency)) throw new IllegalArgumentException("club creation requires ROOM_CARD");
        if (minimumRoomCardBalance < 0 || chargeRoomCardAmount < 0)
            throw new IllegalArgumentException("club creation amounts must not be negative");
        if (policyVersion <= 0) throw new IllegalArgumentException("club creation policy version must be positive");
        if (contentHash == null || !contentHash.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("club creation policy hash is invalid");
    }
}
