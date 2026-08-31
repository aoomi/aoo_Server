package com.aoo.bcg.gateway;

/** Server-authoritative admission port. Client risk assertions are never decisions. */
public interface RiskAdmissionAuthority {
    enum Action { LOGIN, CREATE_ROOM, JOIN_ROOM }

    Decision decide(Action action, long playerId, String deviceId, Long roomId, String requestId);

    record Decision(boolean allowed, String publicCode) {
        public Decision {
            if (publicCode == null || publicCode.isBlank()) throw new IllegalArgumentException("publicCode required");
        }
        public static Decision allow() { return new Decision(true, "OK"); }
        public static Decision reject() { return new Decision(false, "ADMISSION_REJECTED"); }
    }
}
