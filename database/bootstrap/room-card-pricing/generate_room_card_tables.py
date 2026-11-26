#!/usr/bin/env python3
"""Validate the authoritative play registry and emit safe per-play room-card SQL."""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path

PLAY_RE = re.compile(r"^[a-z][a-z0-9]{2,31}$")
ROOT = Path(__file__).resolve().parent
BOOTSTRAP_MANIFEST = ROOT.parent / "manifest.json"


def canonical(value: object) -> str:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":"))


def authority_catalog() -> dict[str, dict]:
    raw = json.loads(BOOTSTRAP_MANIFEST.read_text())
    rows = {}
    for publication in raw["publications"]:
        catalog = publication["catalog"]
        code = catalog["gameCode"]
        if not PLAY_RE.fullmatch(code) or code in rows:
            raise ValueError(f"unsafe or duplicate manifest gameCode: {code!r}")
        rows[code] = catalog
    if len(rows) != 32:
        raise ValueError(f"expected exactly 32 manifest plays, got {len(rows)}")
    return rows


def read_active(path: Path) -> dict[str, dict]:
    rows = {}
    for line in path.read_text().splitlines():
        code, _display, game_id, version, release, generation, source_hash = line.split("\t")
        if not PLAY_RE.fullmatch(code) or code in rows:
            raise ValueError(f"unsafe or duplicate active gameCode: {code!r}")
        if not re.fullmatch(r"[0-9a-f]{64}", source_hash):
            raise ValueError(f"invalid sourceHash for {code}")
        rows[code] = {
            "gameId": int(game_id), "playVersion": version,
            "releaseId": int(release), "indexGeneration": int(generation),
            "runtimeSourceHash": source_hash,
        }
    if len(rows) != 32:
        raise ValueError(f"expected exactly 32 ACTIVE plays, got {len(rows)}")
    return rows


def default_document(code: str, catalog: dict, active: dict) -> dict:
    policy = {
        "policyKey": f"{code}:play-default:hall-owner-fixed-v1",
        "matchScope": "PLAY_DEFAULT", "payerMode": "OWNER",
        "billingKind": "FIXED", "billingContext": "HALL",
        "currencyCode": "ROOM_CARD", "amountMinor": 0,
        "status": "ACTIVE", "confirmationStatus": "CONFIRMED",
        "freePolicyRef": "ROOM_CARD_ALWAYS_FREE_V1",
        "refundPolicyRef": "ROOM_CARD_ZERO_AMOUNT_NO_LEDGER_V1",
        "policyVersion": 1,
    }
    policy["contentHash"] = hashlib.sha256(canonical(policy).encode()).hexdigest()
    return {
        "schemaVersion": 1, "playCode": code,
        "displayName": catalog["displayName"], "gameId": active["gameId"],
        "playVersion": active["playVersion"], "releaseId": active["releaseId"],
        "indexGeneration": active["indexGeneration"],
        "runtimeSourceHash": active["runtimeSourceHash"],
        "tableName": f"aoo_room_card_price_{code}", "policies": [policy],
    }


def initialize(active_tsv: Path) -> None:
    catalog, active = authority_catalog(), read_active(active_tsv)
    if set(catalog) != set(active):
        raise ValueError(f"manifest/ACTIVE play set mismatch: {sorted(set(catalog) ^ set(active))}")
    (ROOT / "plays").mkdir(parents=True, exist_ok=True)
    docs = []
    for code in sorted(catalog):
        if catalog[code]["gameId"] != active[code]["gameId"]:
            raise ValueError(f"gameId mismatch for {code}")
        doc = default_document(code, catalog[code], active[code])
        (ROOT / "plays" / f"{code}.json").write_text(json.dumps(doc, ensure_ascii=False, indent=2) + "\n")
        docs.append({k: doc[k] for k in ("playCode", "displayName", "gameId", "playVersion", "releaseId", "indexGeneration", "runtimeSourceHash", "tableName")})
    registry = {"schemaVersion": 1, "authority": "aoo_game_catalog.game_code + ACTIVE compiled index tuple", "plays": docs}
    (ROOT / "manifest.json").write_text(json.dumps(registry, ensure_ascii=False, indent=2) + "\n")


def load_documents() -> list[dict]:
    registry = json.loads((ROOT / "manifest.json").read_text())
    docs = [json.loads(p.read_text()) for p in sorted((ROOT / "plays").glob("*.json"))]
    if len(docs) != 32 or len(registry["plays"]) != 32:
        raise ValueError("registry and plays must each contain exactly 32 entries")
    expected = {p["playCode"]: p for p in registry["plays"]}
    for doc in docs:
        code = doc["playCode"]
        if not PLAY_RE.fullmatch(code) or doc["tableName"] != f"aoo_room_card_price_{code}":
            raise ValueError(f"unsafe table mapping for {code!r}")
        if code not in expected or len(doc["policies"]) != 1:
            raise ValueError(f"registry/policy mismatch for {code}")
        policy = doc["policies"][0]
        required = (policy["matchScope"] == "PLAY_DEFAULT" and policy["amountMinor"] == 0
                    and policy["status"] == "ACTIVE" and policy["confirmationStatus"] == "CONFIRMED"
                    and bool(policy["freePolicyRef"]) and bool(policy["refundPolicyRef"]))
        if not required:
            raise ValueError(f"invalid zero-price default for {code}")
    return docs


def q(value: str) -> str:
    return "'" + value.replace("'", "''") + "'"


def emit_sql() -> str:
    docs = load_documents()
    parts = ["-- generated structure only; mutable prices are loaded from room-card-pricing/plays/*.json",
             "CREATE TABLE IF NOT EXISTS aoo_room_card_play_registry (play_code VARCHAR(32) PRIMARY KEY, game_id BIGINT UNSIGNED NOT NULL, play_version VARCHAR(64) NOT NULL, display_name VARCHAR(128) NOT NULL, table_name VARCHAR(64) NOT NULL UNIQUE, status VARCHAR(16) NOT NULL, release_id BIGINT UNSIGNED NOT NULL, index_generation BIGINT UNSIGNED NOT NULL, runtime_source_hash CHAR(64) NOT NULL, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), UNIQUE KEY uq_room_card_registry_identity(game_id, play_version)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"]
    for doc in docs:
        table = doc["tableName"]
        parts.append(f"CREATE TABLE IF NOT EXISTS `{table}` (policy_uid CHAR(64) PRIMARY KEY, policy_key VARCHAR(191) NOT NULL UNIQUE, play_code VARCHAR(32) NOT NULL, play_version VARCHAR(64) NOT NULL, match_scope VARCHAR(24) NOT NULL, player_count INT UNSIGNED NULL, measure_kind VARCHAR(32) NULL, measure_value INT UNSIGNED NULL, payer_mode VARCHAR(16) NOT NULL, billing_kind VARCHAR(16) NOT NULL, billing_context VARCHAR(16) NOT NULL, currency_code VARCHAR(32) NOT NULL, amount_minor BIGINT NOT NULL, policy_version BIGINT UNSIGNED NOT NULL, content_hash CHAR(64) NOT NULL, status VARCHAR(16) NOT NULL, confirmation_status VARCHAR(16) NOT NULL, free_policy_ref VARCHAR(128) NULL, refund_policy_ref VARCHAR(128) NOT NULL, release_id BIGINT UNSIGNED NOT NULL, index_generation BIGINT UNSIGNED NOT NULL, runtime_source_hash CHAR(64) NOT NULL, created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3), updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3), CHECK (amount_minor >= 0), CHECK ((match_scope='PLAY_DEFAULT' AND player_count IS NULL AND measure_kind IS NULL AND measure_value IS NULL) OR match_scope='EXACT')) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;")
    return "\n\n".join(parts) + "\n"


def emit_seed() -> str:
    docs = load_documents()
    statements = ["START TRANSACTION;"]
    for doc in docs:
        p = doc["policies"][0]
        statements.append("INSERT INTO aoo_room_card_play_registry(play_code,game_id,play_version,display_name,table_name,status,release_id,index_generation,runtime_source_hash) VALUES(" + ",".join([q(doc["playCode"]),str(doc["gameId"]),q(doc["playVersion"]),q(doc["displayName"]),q(doc["tableName"]),q("ACTIVE"),str(doc["releaseId"]),str(doc["indexGeneration"]),q(doc["runtimeSourceHash"])]) + ") ON DUPLICATE KEY UPDATE game_id=VALUES(game_id),play_version=VALUES(play_version),display_name=VALUES(display_name),table_name=VALUES(table_name),status=VALUES(status),release_id=VALUES(release_id),index_generation=VALUES(index_generation),runtime_source_hash=VALUES(runtime_source_hash);")
        uid = hashlib.sha256((doc["playCode"] + "\0" + p["policyKey"]).encode()).hexdigest()
        vals = [q(uid),q(p["policyKey"]),q(doc["playCode"]),q(doc["playVersion"]),q(p["matchScope"]),"NULL","NULL","NULL",q(p["payerMode"]),q(p["billingKind"]),q(p["billingContext"]),q(p["currencyCode"]),str(p["amountMinor"]),str(p["policyVersion"]),q(p["contentHash"]),q(p["status"]),q(p["confirmationStatus"]),q(p["freePolicyRef"]),q(p["refundPolicyRef"]),str(doc["releaseId"]),str(doc["indexGeneration"]),q(doc["runtimeSourceHash"])]
        statements.append(f"INSERT INTO `{doc['tableName']}`(policy_uid,policy_key,play_code,play_version,match_scope,player_count,measure_kind,measure_value,payer_mode,billing_kind,billing_context,currency_code,amount_minor,policy_version,content_hash,status,confirmation_status,free_policy_ref,refund_policy_ref,release_id,index_generation,runtime_source_hash) VALUES(" + ",".join(vals) + ") ON DUPLICATE KEY UPDATE amount_minor=VALUES(amount_minor),policy_version=VALUES(policy_version),content_hash=VALUES(content_hash),status=VALUES(status),confirmation_status=VALUES(confirmation_status),free_policy_ref=VALUES(free_policy_ref),refund_policy_ref=VALUES(refund_policy_ref),release_id=VALUES(release_id),index_generation=VALUES(index_generation),runtime_source_hash=VALUES(runtime_source_hash);")
    statements.append("COMMIT;")
    return "\n".join(statements) + "\n"


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("command", choices=("initialize", "validate", "emit-ddl", "emit-seed"))
    parser.add_argument("--active-tsv", type=Path)
    args = parser.parse_args()
    if args.command == "initialize":
        if not args.active_tsv: parser.error("initialize requires --active-tsv")
        initialize(args.active_tsv)
    elif args.command == "validate": load_documents()
    elif args.command == "emit-ddl": print(emit_sql(), end="")
    else: print(emit_seed(), end="")


if __name__ == "__main__":
    main()
