#!/usr/bin/env python3
"""Capture and compare F03 MongoDB/MySQL persistence evidence."""

import argparse
import json
import os
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "work" / "python"))
from pymongo import MongoClient


TABLES = (
    "clark_game_new.player",
    "aoo_smoke_game.player",
    "clark_game_new.dbclublist",
    "aoo_smoke_game.dbclublist",
    "clark_game_new.playerroomalone",
    "aoo_smoke_game.playerroomalone",
    "clark_game_new.aoo_room_event",
    "clark_game_new.aoo_room_snapshot",
    "aoo_smoke_game.aoo_room_event",
    "aoo_smoke_game.aoo_room_snapshot",
)


def run(command):
    result = subprocess.run(command, check=False, capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError(result.stderr.strip() or result.stdout.strip())
    return result.stdout


def capture(args):
    client = MongoClient(args.mongo_uri, serverSelectionTimeoutMS=5000)
    current_database = client[args.mongo_database]
    legacy_database = client[args.legacy_mongo_database]
    current_key = current_database["DbTagDBDataKey"].find_one({"_id": 1}) or {}
    legacy_key = legacy_database["tagdbdatakeys"].find_one({"_id": 1}) or {}
    mongo = {
        "database": args.mongo_database,
        "accountInfos": current_database["DbTagAccountInfo"].count_documents({}),
        "accountTypes": current_database["DbTagAccountType"].count_documents({}),
        "sequence": int(current_key.get("AccountID", 0)),
        "legacyDatabase": args.legacy_mongo_database,
        "legacyAccountInfos": legacy_database["tagaccountinfos"].count_documents({}),
        "legacySequence": int(legacy_key.get("AccountID", 0)),
    }

    unions = " UNION ALL ".join(
        f"SELECT '{table}',COUNT(*) FROM {table}" for table in TABLES
    )
    mysql_base = [
        "docker", "exec", args.mysql_container, "mysql", "-uroot",
        f"-p{args.mysql_password}", "--batch", "--skip-column-names",
    ]
    counts = {}
    for line in run(mysql_base + ["-e", unions + ";"]).splitlines():
        name, value = line.split("\t", 1)
        counts[name] = int(value)
    health_output = run(mysql_base + ["-e", "CHECK TABLE " + ",".join(TABLES) + ";"])
    unhealthy = []
    for line in health_output.splitlines():
        columns = line.split("\t")
        if len(columns) >= 4 and columns[2] == "status" and columns[3] != "OK":
            unhealthy.append({"table": columns[0], "status": columns[3]})
        elif len(columns) >= 4 and columns[2] in {"error", "Error"}:
            unhealthy.append({"table": columns[0], "status": columns[3]})
    return {
        "capturedAt": datetime.now(timezone.utc).astimezone().isoformat(),
        "mongo": mongo,
        "mysqlCounts": counts,
        "unhealthyTables": unhealthy,
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--mysql-container", default="aoo-mysql")
    parser.add_argument("--mongo-uri", default="mongodb://127.0.0.1:27017")
    parser.add_argument("--mongo-database", default="clark_mongodb")
    parser.add_argument("--legacy-mongo-database", default="Account01_aoo")
    parser.add_argument(
        "--mysql-password", default=os.getenv("AOO_MIGRATION_MYSQL_PASSWORD")
    )
    parser.add_argument("--output", type=Path)
    parser.add_argument("--compare", type=Path)
    args = parser.parse_args()
    if not args.mysql_password:
        parser.error("set AOO_MIGRATION_MYSQL_PASSWORD or --mysql-password")

    current = capture(args)
    failures = []
    growth = None
    if args.compare:
        baseline_document = json.loads(args.compare.read_text(encoding="utf-8"))
        baseline = baseline_document.get("current", baseline_document)
        growth = {
            "mongo.accountInfos": (
                current["mongo"]["accountInfos"] - baseline["mongo"]["accountInfos"]
            ),
            "mysql.players": (
                current["mysqlCounts"]["clark_game_new.player"]
                + current["mysqlCounts"]["aoo_smoke_game.player"]
                - baseline["mysqlCounts"]["clark_game_new.player"]
                - baseline["mysqlCounts"]["aoo_smoke_game.player"]
            ),
            "mysql.clubs": (
                current["mysqlCounts"]["clark_game_new.dbclublist"]
                + current["mysqlCounts"]["aoo_smoke_game.dbclublist"]
                - baseline["mysqlCounts"]["clark_game_new.dbclublist"]
                - baseline["mysqlCounts"]["aoo_smoke_game.dbclublist"]
            ),
            "mysql.roomRecords": (
                current["mysqlCounts"]["clark_game_new.playerroomalone"]
                + current["mysqlCounts"]["aoo_smoke_game.playerroomalone"]
                - baseline["mysqlCounts"]["clark_game_new.playerroomalone"]
                - baseline["mysqlCounts"]["aoo_smoke_game.playerroomalone"]
            ),
        }
        required_growth = (
            "mongo.accountInfos",
            "mysql.players",
            "mysql.clubs",
        )
        for name in required_growth:
            value = growth[name]
            if value <= 0:
                failures.append(f"{name} did not increase")
    if current["unhealthyTables"]:
        failures.append("one or more MySQL tables are unhealthy")
    report = {
        "status": "PASSED" if not failures else "FAILED",
        "current": current,
        "growth": growth,
        "failures": failures,
    }
    rendered = json.dumps(report, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered, encoding="utf-8")
    print(rendered, end="")
    return 0 if not failures else 1


if __name__ == "__main__":
    raise SystemExit(main())
