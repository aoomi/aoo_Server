package com.ddm.server.protocol.v2;

public final class ProtocolRequestScope implements AutoCloseable {
    public enum Version { V1, V2 }
    private static final ThreadLocal<Version> CURRENT = ThreadLocal.withInitial(() -> Version.V2);
    private final Version previous;

    private ProtocolRequestScope(Version version) {
        previous = CURRENT.get();
        CURRENT.set(version);
    }

    public static ProtocolRequestScope enter(Version version) { return new ProtocolRequestScope(version); }
    public static Version current() { return CURRENT.get(); }

    @Override
    public void close() { CURRENT.set(previous); }
}
