#!/usr/bin/env python3
"""Two-player room preflight for the first poker and mahjong games."""

import json
import uuid

from f03_local_protocol_smoke import GameSocket, register, verify_legacy_auth


GAMES = (
    (
        "NJPDK", 9996, "njpdk", "SNJPDK_SetStart",
        {
            "jushu": 8, "renshu": 2, "setCount": 8,
            "playerNum": 2, "playerMinNum": 2,
            "paymentRoomCardType": 0, "createType": 1,
            "kexuanwanfa": [], "gaoji": [], "fangjian": [],
            "paixing": [], "teshu": [], "sign": 1,
        },
    ),
    (
        "CDXZMJ", 19996, "cdxzmj", "SCDXZMJ_SetStart",
        {
            "jushu": 4, "renshu": 2, "setCount": 4,
            "playerNum": 2, "playerMinNum": 2,
            "paymentRoomCardType": 0, "createType": 1,
            "kexuanwanfa": [], "gaoji": [], "fangjian": [],
            "sign": 1, "fengDing": 0,
        },
    ),
)


def response_body(payload, event):
    marker = event.encode()
    start = payload.index(marker) + len(marker)
    length = int.from_bytes(payload[start + 2:start + 6], "big")
    raw = payload[start + 6:start + 6 + length]
    try:
        return json.loads(raw.decode()) if raw else {}
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
        raise AssertionError(f"non-JSON response for {event}: {raw!r}") from error


def login_player(name, port, suffix):
    account_id, token = register(f"f03-{name.lower()}-{suffix}-{uuid.uuid4()}")
    verify_legacy_auth(account_id, token)
    connection = GameSocket(port)
    assert connection.handshake == "HTTP/1.1 101 Switching Protocols"
    connection.send(
        "base.c1004login",
        {
            "accountID": account_id, "openid": "", "unionid": "",
            "token": token, "nickName": "", "sex": 0,
            "headImageUrl": "", "serverID": 0, "version": "1.0.1",
            "isMobile": 0,
        },
        1,
    )
    connection.receive_event("base.c1004login")
    connection.send(
        "base.c1001createrole",
        {
            "nickName": f"F03{suffix}{account_id}", "sex": 1,
            "headImageUrl": "", "accountID": account_id,
            "isMobile": 0, "Phone": 0,
        },
        2,
    )
    connection.receive_event("base.c1001createrole")
    connection.send("base.c1006rolelogin", {"accountID": account_id}, 3)
    connection.receive_event("base.c1006rolelogin")
    return connection, account_id


def verify_game(name, port, prefix, start_event, create_config):
    owner, owner_id = login_player(name, port, "Owner")
    create_event = f"{prefix}.c{name.lower()}createroom"
    owner.send(create_event, create_config, 4)
    created_payload = owner.receive_event(create_event)
    created = response_body(created_payload, create_event)
    room_id = int(created["roomID"])
    room_key = str(created["roomKey"])

    guest, guest_id = login_player(name, port, "Guest")
    enter_event = f"{prefix}.c{name.lower()}enterroom"
    guest.send(
        enter_event,
        {"roomKey": room_key, "posID": -1, "clubId": 0, "password": "", "existQuickJoin": False},
        4,
    )
    guest.receive_event(enter_event)

    ready_event = f"{prefix}.c{name.lower()}readyroom"
    owner.send(ready_event, {"roomID": room_id}, 5)
    owner.receive_event(ready_event)
    guest.send(ready_event, {"roomID": room_id}, 5)
    guest.receive_event(ready_event)

    # Room startup runs on the room scheduler. A ready acknowledgement alone is
    # not sufficient evidence that cards were dealt and the set really started.
    owner.receive_event(start_event, attempts=20)
    guest.receive_event(start_event, attempts=20)

    owner.close()
    guest.close()
    print(
        f"PASS {name}: room={room_key}/{room_id}, "
        f"owner={owner_id}, guest={guest_id}, create/join/ready/set-start"
    )


def main():
    for game in GAMES:
        verify_game(*game)
    print("F03 two-player room preflight passed")


if __name__ == "__main__":
    main()
