#!/usr/bin/env python3
import json, sys, time, urllib.request, uuid
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from f03_two_player_room_smoke import GameSocket, register, response_body, verify_legacy_auth

ACCOUNT_URL = "http://127.0.0.1:904"
PORT = 9996
EVENT_PREFIX = "njpdk"
PLAY_VERSION = "legacy-equivalent-1"
STATE_FILE = Path("work/runtime/f03-v2-authority-recovery.json")

def ticket(device_id, token):
    request_id = "ticket-" + uuid.uuid4().hex
    envelope = {"protocolVersion":"2.0","msgId":"account.ws_ticket","kind":"req",
        "requestId":request_id,"seq":1,"traceId":request_id,"timestamp":int(time.time()*1000),
        "body":{"deviceId":device_id}}
    request = urllib.request.Request(ACCOUNT_URL + "/api/v2/account/dispatch",
        data=json.dumps(envelope).encode(), headers={"Content-Type":"application/json",
        "Authorization":"Bearer " + token}, method="POST")
    response = json.load(urllib.request.urlopen(request, timeout=10))
    assert response["code"] == 0
    return response["body"]["wsTicket"]

def connect(account_id, token, create_role=False):
    verify_legacy_auth(account_id, token)
    connection = GameSocket(PORT)
    connection.send("base.c1004login", {"accountID":account_id,"openid":"","unionid":"",
        "token":token,"nickName":"","sex":0,"headImageUrl":"","serverID":0,
        "version":"1.0.1","isMobile":0}, 1)
    connection.receive_event("base.c1004login")
    if create_role:
        connection.send("base.c1001createrole", {"nickName":"V2"+str(account_id),"sex":1,
            "headImageUrl":"","accountID":account_id,"isMobile":0,"Phone":0}, 2)
        connection.receive_event("base.c1001createrole")
    connection.send("base.c1006rolelogin", {"accountID":account_id}, 3)
    connection.receive_event("base.c1006rolelogin")
    return connection

def v2(connection, account_id, token, device_id, room_id, sequence, command, body=None):
    request_id = "v2-" + uuid.uuid4().hex
    envelope = {"protocolVersion":"2.0","msgId":"poker.njpdk.dispatch","kind":"req",
        "requestId":request_id,"seq":sequence,"traceId":request_id,
        "timestamp":int(time.time()*1000),"roomId":str(room_id),"roundNo":1,
        "playVersion":PLAY_VERSION,"wsTicket":ticket(device_id, token),
        "body":{"command":command, **(body or {})}}
    connection.send("protocol.v2.dispatch", envelope, 20 + sequence)
    payload = connection.receive_event("protocol.v2.dispatch", attempts=20)
    response = response_body(payload, "protocol.v2.dispatch")
    assert response["code"] == 0, response
    return response["body"]["body"] if "body" in response.get("body", {}) else response["body"]

def create():
    owner_device="v2-owner-"+uuid.uuid4().hex; guest_device="v2-guest-"+uuid.uuid4().hex
    owner_id,owner_token=register(owner_device); guest_id,guest_token=register(guest_device)
    owner=connect(owner_id,owner_token,True); guest=connect(guest_id,guest_token,True)
    create_event="njpdk.cnjpdkcreateroom"
    owner.send(create_event,{"jushu":8,"renshu":2,"setCount":8,"playerNum":2,
        "playerMinNum":2,"paymentRoomCardType":0,"createType":1,"kexuanwanfa":[],
        "gaoji":[],"fangjian":[],"paixing":[],"teshu":[],"sign":1},4)
    created=response_body(owner.receive_event(create_event),create_event)
    room_id=int(created["roomID"]); room_key=str(created["roomKey"])
    enter_event="njpdk.cnjpdkenterroom"
    guest.send(enter_event,{"roomKey":room_key,"posID":-1,"clubId":0,"password":"","existQuickJoin":False},4)
    guest.receive_event(enter_event)
    v2(owner,owner_id,owner_token,owner_device,room_id,1,"join")
    v2(guest,guest_id,guest_token,guest_device,room_id,1,"join")
    expected=v2(owner,owner_id,owner_token,owner_device,room_id,2,"start")
    STATE_FILE.parent.mkdir(parents=True,exist_ok=True)
    STATE_FILE.write_text(json.dumps({"roomId":room_id,"ownerId":owner_id,"ownerToken":owner_token,
        "ownerDevice":owner_device,"expected":expected},ensure_ascii=False),encoding="utf-8")
    owner.close(); guest.close(); print(f"PASS V2 create room={room_id}, snapshot expected")

def verify():
    saved=json.loads(STATE_FILE.read_text(encoding="utf-8")); owner=connect(saved["ownerId"],saved["ownerToken"],False)
    actual=v2(owner,saved["ownerId"],saved["ownerToken"],saved["ownerDevice"],saved["roomId"],1,"state")
    owner.close(); assert actual == saved["expected"], (actual,saved["expected"])
    print(f"PASS V2 recovered room={saved['roomId']} authority state identical")

if __name__ == "__main__":
    {"create":create,"verify":verify}[sys.argv[1]]()
