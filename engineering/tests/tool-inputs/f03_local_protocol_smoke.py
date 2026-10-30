#!/usr/bin/env python3
"""Repeatable local preflight for the F03 account/game WebSocket boundary."""

import base64
import json
import os
import socket
import struct
import time
import urllib.error
import urllib.request
import uuid


ACCOUNT_URL = "http://127.0.0.1:904"
SERVERS = (("Hall", 9998), ("NJPDK", 9996), ("CDXZMJ", 19996))


def register(device_id):
    request_id = f"f03-{uuid.uuid4()}"
    envelope = {
        "protocolVersion": "2.0",
        "msgId": "account.login_compat",
        "kind": "req",
        "requestId": request_id,
        "seq": 1,
        "traceId": request_id,
        "timestamp": int(time.time() * 1000),
        "body": {
            "action": "one_key_register",
            "deviceId": device_id,
            "payload": {"Head": 0xFF00},
        },
    }
    request = urllib.request.Request(
        f"{ACCOUNT_URL}/api/v2/account/dispatch",
        data=json.dumps(envelope).encode(),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    response = json.load(urllib.request.urlopen(request, timeout=10))
    assert response["code"] == 0 and response["kind"] == "resp"
    payload = response["body"]["payload"]
    assert response["body"]["wsTicket"]
    return int(payload["AccountID"]), payload["Token"]


def verify_legacy_auth(account_id, token):
    body = {
        "_Head": "java.0x000D.account",
        "AccountID": account_id,
        "Token": token,
        "ServerID": 0,
    }
    request = urllib.request.Request(
        f"{ACCOUNT_URL}/JavaServerPack?Sign=DDCat&ServerName=JavaServer",
        data=json.dumps(body).encode(),
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    response = json.load(urllib.request.urlopen(request, timeout=10))
    assert response["Code"] == 0 and response["AccountID"] == account_id


class GameSocket:
    def __init__(self, port, origin=None):
        self.socket = socket.create_connection(("127.0.0.1", port), timeout=5)
        key = base64.b64encode(os.urandom(16)).decode()
        origin_header = f"Origin: {origin}\r\n" if origin else ""
        request = (
            f"GET / HTTP/1.1\r\nHost: 127.0.0.1:{port}\r\n"
            "Upgrade: websocket\r\nConnection: Upgrade\r\n"
            f"Sec-WebSocket-Key: {key}\r\nSec-WebSocket-Version: 13\r\n"
            f"{origin_header}\r\n"
        )
        self.socket.sendall(request.encode())
        self.handshake = self.socket.recv(4096).split(b"\r\n", 1)[0].decode()

    def send(self, event, body, seq):
        event_bytes = event.encode()
        body_bytes = json.dumps(body, separators=(",", ":")).encode()
        payload = (
            bytes((2,))
            + struct.pack(">H", seq)
            + struct.pack(">H", len(event_bytes))
            + event_bytes
            + struct.pack(">H", len(body_bytes))
            + body_bytes
        )
        mask = os.urandom(4)
        if len(payload) < 126:
            header = bytes((0x82, 0x80 | len(payload)))
        elif len(payload) < 65536:
            header = bytes((0x82, 0xFE)) + struct.pack(">H", len(payload))
        else:
            header = bytes((0x82, 0xFF)) + struct.pack(">Q", len(payload))
        masked = bytes(value ^ mask[index % 4] for index, value in enumerate(payload))
        self.socket.sendall(header + mask + masked)

    def receive(self):
        self.socket.settimeout(8)
        first = self.socket.recv(2)
        assert len(first) == 2
        length = first[1] & 0x7F
        if length == 126:
            length = struct.unpack(">H", self.socket.recv(2))[0]
        elif length == 127:
            length = struct.unpack(">Q", self.socket.recv(8))[0]
        payload = b""
        while len(payload) < length:
            payload += self.socket.recv(length - len(payload))
        return first[0] & 0x0F, payload

    def receive_event(self, event, attempts=5):
        needle = event.encode()
        for _ in range(attempts):
            opcode, payload = self.receive()
            if opcode == 2 and needle in payload:
                return payload
        raise AssertionError(f"did not receive {event}")

    def close(self):
        self.socket.close()


def verify_rejected_origin(port):
    connection = GameSocket(port, "https://attacker.invalid")
    assert connection.handshake == "HTTP/1.1 403 Forbidden"
    connection.close()


def verify_login(name, port):
    account_id, token = register(f"f03-{name.lower()}-{uuid.uuid4()}")
    verify_legacy_auth(account_id, token)
    verify_rejected_origin(port)
    connection = GameSocket(port)
    assert connection.handshake == "HTTP/1.1 101 Switching Protocols"
    connection.send(
        "base.c1004login",
        {
            "accountID": account_id,
            "openid": "",
            "unionid": "",
            "token": token,
            "nickName": "",
            "sex": 0,
            "headImageUrl": "",
            "serverID": 0,
            "version": "1.0.1",
            "isMobile": 0,
        },
        1,
    )
    login = connection.receive_event("base.c1004login")
    assert b"base.c1004login" in login and b"legacy protocol is disabled" not in login, login
    connection.send(
        "base.c1001createrole",
        {
            "nickName": f"F03Player{account_id}",
            "sex": 1,
            "headImageUrl": "",
            "accountID": account_id,
            "isMobile": 0,
            "Phone": 0,
        },
        2,
    )
    # Role creation is persisted by the server task queue. Wait for its
    # response before issuing role login so the smoke test follows the same
    # ordering contract as a real client.
    connection.receive_event("base.c1001createrole")
    connection.send("base.c1006rolelogin", {"accountID": account_id}, 3)
    role = connection.receive_event("base.c1006rolelogin")
    assert f'"accountID":{account_id}'.encode() in role and b'"pid":' in role, role.hex()
    if name == "Hall":
        connection.send("club.cgetclublist", {}, 4)
        connection.receive_event("club.cgetclublist")
        club_name = f"F03Club{account_id}"
        connection.send("club.cclubcreate", {"clubName": club_name, "cityId": 0}, 5)
        created = connection.receive_event("club.cclubcreate")
        assert club_name.encode() in created, created.hex()
        connection.send("club.cgetclublist", {}, 6)
        clubs = connection.receive_event("club.cgetclublist")
        assert club_name.encode() in clubs, clubs.hex()
    elif name == "NJPDK":
        connection.send(
            "njpdk.cnjpdkcreateroom",
            {
                "jushu": 8, "renshu": 2, "setCount": 8,
                "playerNum": 2, "playerMinNum": 2,
                "paymentRoomCardType": 0, "createType": 1,
                "kexuanwanfa": [], "gaoji": [], "fangjian": [],
                "paixing": [], "teshu": [], "sign": 1,
            },
            4,
        )
        room = connection.receive_event("njpdk.cnjpdkcreateroom")
        assert b'"roomKey"' in room and b'"roomID"' in room, room.hex()
        connection.send("room.cbaseexitroom", {}, 5)
        connection.receive_event("room.cbaseexitroom")
    elif name == "CDXZMJ":
        connection.send(
            "cdxzmj.ccdxzmjcreateroom",
            {
                "jushu": 4, "renshu": 2, "setCount": 4,
                "playerNum": 2, "playerMinNum": 2,
                "paymentRoomCardType": 0, "createType": 1,
                "kexuanwanfa": [], "gaoji": [], "fangjian": [],
                "sign": 1, "fengDing": 0,
            },
            4,
        )
        room = connection.receive_event("cdxzmj.ccdxzmjcreateroom")
        assert b'"roomKey"' in room and b'"roomID"' in room, room.hex()
        connection.send("room.cbaseexitroom", {}, 5)
        connection.receive_event("room.cbaseexitroom")
    connection.close()
    scope = "auth/origin/login/create-role/role-login"
    if name == "Hall":
        scope += "/club-list-create-refresh"
    else:
        scope += "/create-room/exit-room"
    print(f"PASS {name}: account={account_id}, {scope}")


def main():
    for name, port in SERVERS:
        verify_login(name, port)
    print("F03 local protocol preflight passed")


if __name__ == "__main__":
    main()
