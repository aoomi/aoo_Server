package com.aoo.bcg.hall.room;

import com.aoo.bcg.lobby.LobbyRoomCreateSaga;

/** Compatibility facade for the former Hall saga owner. */
public class RoomCreateSaga extends LobbyRoomCreateSaga {
    public RoomCreateSaga(Store store, BillingPort billing, RoomPort rooms,
            RuleNormalizer normalizer, AuthorityPort authority) {
        super(store, billing, rooms, normalizer, authority);
    }
}
