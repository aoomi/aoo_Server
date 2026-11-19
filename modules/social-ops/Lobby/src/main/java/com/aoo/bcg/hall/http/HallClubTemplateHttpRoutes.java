package com.aoo.bcg.hall.http;

import com.aoo.bcg.hall.room.JdbcHallRepository;
import com.aoo.bcg.hall.room.RoomCreateSaga;
import com.aoo.bcg.lobby.LobbyRequestAuthenticator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

/** Compatibility facade; Lobby owns the sole club-template HTTP context. */
public final class HallClubTemplateHttpRoutes {
    private HallClubTemplateHttpRoutes() {}

    @FunctionalInterface
    public interface TemplateResolver extends com.aoo.bcg.lobby.LobbyClubTemplateHttpRoutes.TemplateResolver {}

    public static void mount(HttpServer server, JdbcHallRepository hall, LobbyRequestAuthenticator auth,
            ObjectMapper json, RoomCreateSaga saga, TemplateResolver resolver) {
        com.aoo.bcg.lobby.LobbyClubTemplateHttpRoutes.mount(
                server, hall, auth, json, saga, resolver);
    }
}
