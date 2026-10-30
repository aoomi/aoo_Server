package com.aoo.bcg.hall.http;

import java.time.Clock;
import javax.sql.DataSource;

/** Compatibility facade; Lobby owns the account bearer/HMAC verification implementation. */
public final class HallRequestAuthenticator {
    private final com.aoo.bcg.lobby.LobbyRequestAuthenticator delegate;

    public HallRequestAuthenticator(String secret, Clock clock) {
        this.delegate = new com.aoo.bcg.lobby.LobbyRequestAuthenticator(secret, clock);
    }

    public HallRequestAuthenticator(DataSource sessions, Clock clock) {
        this.delegate = new com.aoo.bcg.lobby.LobbyRequestAuthenticator(sessions, clock);
    }

    public long authenticate(String authorization) {
        return delegate.authenticate(authorization);
    }

    public byte[] sign(String value) throws Exception {
        return delegate.sign(value);
    }
}
