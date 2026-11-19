package com.aoo.bcg.hall.room;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.aoo.bcg.lobby.LobbyRoomCreateSagaStore;
import javax.sql.DataSource;

/** Compatibility facade; durable saga SQL is owned by Lobby. */
public class JdbcRoomCreateSagaStore extends LobbyRoomCreateSagaStore implements RoomCreateSaga.Store {
    public JdbcRoomCreateSagaStore(DataSource dataSource, ObjectMapper json) {
        super(dataSource, json);
    }
}
