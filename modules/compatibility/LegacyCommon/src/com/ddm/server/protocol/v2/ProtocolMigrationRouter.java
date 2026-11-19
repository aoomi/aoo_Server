package com.ddm.server.protocol.v2;

/** Production protocol selector. Legacy rollback was removed after the V2 cutover. */
public final class ProtocolMigrationRouter {
    public enum Mode { V2 }

    public Mode route(String stage, String msgId) {
        return Mode.V2;
    }

    public void setDefaultMode(Mode mode) { requireV2(mode); }
    public void setStageMode(String stage, Mode mode) { requireV2(mode); }
    public void setMessageMode(String msgId, Mode mode) { requireV2(mode); }

    private static void requireV2(Mode mode) {
        if (mode != Mode.V2) throw new IllegalArgumentException("only protocol V2 is enabled");
    }
}
