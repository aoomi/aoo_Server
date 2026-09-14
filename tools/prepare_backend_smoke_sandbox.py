#!/usr/bin/env python3
import argparse
import os
import re
import shutil
import subprocess
from pathlib import Path


def replace_in_file(path: Path, replacements):
    text = path.read_text(encoding="utf-8")
    for old, new in replacements:
        text = text.replace(old, new)
    path.write_text(text, encoding="utf-8")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--game", required=True)
    parser.add_argument("--game-type", required=True, type=int)
    parser.add_argument("--client-port", required=True, type=int)
    parser.add_argument("--http-port", required=True, type=int)
    parser.add_argument("--node-port", required=True, type=int)
    parser.add_argument("--environment", choices=("test",), required=True)
    parser.add_argument("--apply", action="store_true")
    args = parser.parse_args()
    if not re.fullmatch(r"[A-Za-z0-9_-]{2,32}", args.game):
        raise SystemExit("invalid game name")
    if not all(1024 <= port <= 65535 for port in (args.client_port, args.http_port, args.node_port)):
        raise SystemExit("ports must be within 1024..65535")
    if len({args.client_port, args.http_port, args.node_port}) != 3:
        raise SystemExit("ports must be unique")
    if not args.apply:
        print(f"DRY-RUN game={args.game} gameType={args.game_type} ports={args.client_port},{args.http_port},{args.node_port}")
        return
    password = os.environ.get("AOO_SMOKE_MYSQL_PASSWORD")
    if not password:
        raise SystemExit("AOO_SMOKE_MYSQL_PASSWORD is required for --apply")

    root = Path(__file__).resolve().parents[1]
    source = root / "work/local-runtime/conf-njpdk"
    target = root / f"work/local-runtime/conf-smoke-{args.game.lower()}"
    target.mkdir(parents=True, exist_ok=True)
    shutil.copytree(source, target, dirs_exist_ok=True)

    common = [
        ("clark_game_new", "aoo_smoke_game"),
        ("clark_log_new", "aoo_smoke_log"),
        ("/db_zle?", "/aoo_smoke_zle?"),
        ("?useUnicode=true", "?allowPublicKeyRetrieval=true&useSSL=false&useUnicode=true"),
        ("&autoReconnect=true", "&connectTimeout=5000&socketTimeout=5000"),
        ("testWhileIdle=true", "testWhileIdle=false"),
        ("GameServer.ClientPort=9996", f"GameServer.ClientPort={args.client_port}"),
        ("Server.HttpServer=9886", f"Server.HttpServer={args.http_port}"),
        ("Redis.PORT=16379", "Redis.PORT=26379"),
        ("Redis.AUTH=123456", "Redis.AUTH=aoo_smoke_123"),
        ("nodeName=njpdk-local", f"nodeName=smoke-{args.game.lower()}"),
        ("nodeVipAddress=ws://127.0.0.1:9996", f"nodeVipAddress=ws://127.0.0.1:{args.client_port}"),
        ("nodePort=9997", f"nodePort={args.node_port}"),
    ]
    for name in ("game.properties", "type.properties", "db.properties"):
        path = target / name
        if path.exists():
            replace_in_file(path, common)
            text = path.read_text(encoding="utf-8")
            text = re.sub(r"(?m)^([A-Za-z0-9_.]*(?:USER|username))=.*$", r"\1=aoo_smoke", text)
            text = re.sub(r"(?m)^([A-Za-z0-9_.]*(?:PWD|PASSWORD|password))=.*$", r"\1=aoo_smoke_123", text)
            path.write_text(text, encoding="utf-8")

    replace_in_file(target / "first.properties", [
        ("game_sid=7103", f"game_sid={990000 + args.game_type}"),
    ])
    replace_in_file(target / "redis.properties", [
        ("redis.port=16379", "redis.port=26379"),
        ("redis.password=123456", "redis.password=aoo_smoke_123"),
    ])
    replace_in_file(target / "mq.properties", [
        ("mq.start.consumer=true", "mq.start.consumer=false"),
        ("mq.group.name=njpdk_group", f"mq.group.name=smoke_{args.game.lower()}_group"),
    ])

    sql = (
        "DELETE FROM gamenode WHERE gameId={gt};"
        "DELETE FROM gametype WHERE gametype={gt};"
        "INSERT INTO gametype "
        "(gametype,name,logoico,barColors,gameName,have_xifen,tab,hutypelist,sort,"
        "cityID,provinceID,countyID,classType,gameServerIP,webSocketUrl,gameServerPort,"
        "httpUrl,openType,openContent,multiport) "
        "VALUES ({gt},'{game}','btn_{lower}','C1D011','{lower}',0,1,'',0,"
        "0,0,0,1,'127.0.0.1','ws://127.0.0.1:{client}',{node},"
        "'http://127.0.0.1:{http}',0,'[]',0);"
    ).format(
        gt=args.game_type,
        game=args.game.upper(),
        lower=args.game.lower(),
        client=args.client_port,
        node=args.node_port,
        http=args.http_port,
    )
    subprocess.run([
        "docker", "exec", "-i", "aoo-mysql", "mysql", "-uroot", f"-p{password}",
        "aoo_smoke_game", "-e", sql,
    ], check=True)
    subprocess.run([
        "docker", "exec", "aoo-smoke-redis", "redis-cli",
        "-a", "aoo_smoke_123", "FLUSHALL",
    ], check=True, stdout=subprocess.DEVNULL)
    print(target)


if __name__ == "__main__":
    main()
