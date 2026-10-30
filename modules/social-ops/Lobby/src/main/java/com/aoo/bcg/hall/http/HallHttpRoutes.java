package com.aoo.bcg.hall.http;

import com.aoo.bcg.hall.room.JdbcHallRepository;
import com.aoo.bcg.hall.room.RoomAuthorityRejected;
import com.aoo.bcg.hall.room.RoomCreateSaga;
import com.aoo.bcg.lobby.LobbyRequestAuthenticator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import core.replay.RecordReplayQueryService;
import core.replay.ReplayCodeService;
import java.util.Map;

/**
 * Compatibility facade for the former Hall HTTP owner.
 *
 * <p>LobbyHttpRoutes owns all frozen HTTP contexts and handler behavior. This
 * facade preserves the source and binary entry point for legacy callers while
 * guaranteeing that it cannot register a second context set.</p>
 */
public final class HallHttpRoutes {
    private HallHttpRoutes() {}

    public static void mount(HttpServer server, JdbcHallRepository repository,
            LobbyRequestAuthenticator authenticator, ObjectMapper json, RoomCreateSaga saga,
            RecordReplayQueryService records, ReplayCodeService replayCodes, String internalToken) {
        com.aoo.bcg.lobby.LobbyHttpRoutes.mount(
                server, repository, authenticator, json, saga, records, replayCodes, internalToken);
    }

    /** Compatibility projection retained for existing authority-error tests/callers. */
    public static Map<String, Object> authorityFailure(RoomAuthorityRejected failure, String trace) {
        return com.aoo.bcg.lobby.LobbyHttpRoutes.authorityFailure(failure, trace);
    }

    /** Compatibility reflection seam; route registration remains owned by Lobby. */
    private static boolean activeDiagnosticAllowed(boolean enabled, boolean loopback, String optIn) {
        return enabled && loopback && "1".equals(optIn);
    }
}
